/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package io.mapsmessaging.schemas.formatters.impl.cdr;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class CdrCodec {
  private static final BigInteger TWO_TO_64 = BigInteger.ONE.shiftLeft(64);
  private static final int MAX_ELEMENTS = 1_000_000;

  private CdrCodec() {
  }

  public static void writeFields(CdrWriter writer, List<CdrField> fields, JsonObject values) throws IOException {
    for (CdrField field : fields) {
      String name = field.name();
      if (!values.has(name) || values.get(name).isJsonNull()) {
        throw new IOException("Missing CDR field: " + name);
      }
      writeValue(writer, field, values.get(name));
    }
  }

  public static JsonObject readFields(CdrReader reader, List<CdrField> fields) throws IOException {
    JsonObject values = new JsonObject();
    for (CdrField field : fields) {
      values.add(field.name(), readValue(reader, field));
    }
    return values;
  }

  private static void writeValue(CdrWriter writer, CdrField field, JsonElement value) throws IOException {
    String type = field.type();
    switch (type) {
      case "struct" -> writeFields(writer, field.children(), value.getAsJsonObject());
      case "array", "sequence" -> {
        JsonArray array = value.getAsJsonArray();
        if (type.equals("array") && array.size() != field.length()) {
          throw new IOException("CDR fixed array has incorrect length");
        }
        if (array.size() > MAX_ELEMENTS) {
          throw new IOException("CDR collection too large");
        }
        if (type.equals("sequence")) {
          writer.writeInteger(array.size(), 4);
        }
        for (JsonElement entry : array) {
          writeValue(writer, field.element(), entry);
        }
      }
      case "string" -> {
        byte[] bytes = value.getAsString().getBytes(StandardCharsets.UTF_8);
        if (bytes.length == Integer.MAX_VALUE || value.getAsString().indexOf('\0') >= 0) {
          throw new IOException("Invalid CDR string");
        }
        writer.writeInteger(bytes.length + 1L, 4);
        writer.writeBytes(bytes);
        writer.writeBytes(new byte[]{0});
      }
      case "boolean", "bool" -> writer.writeInteger(value.getAsBoolean() ? 1 : 0, 1);
      case "char" -> {
        String character = value.getAsString();
        if (character.length() != 1 || character.charAt(0) > 255) {
          throw new IOException("CDR char must be one 8-bit character");
        }
        writer.writeInteger(character.charAt(0), 1);
      }
      case "octet", "uint8" -> writeNumber(writer, value, 1, false);
      case "int8" -> writeNumber(writer, value, 1, true);
      case "short", "int16" -> writeNumber(writer, value, 2, true);
      case "ushort", "uint16" -> writeNumber(writer, value, 2, false);
      case "long", "int32" -> writeNumber(writer, value, 4, true);
      case "ulong", "uint32" -> writeNumber(writer, value, 4, false);
      case "longlong", "int64" -> writeNumber(writer, value, 8, true);
      case "ulonglong", "uint64" -> writeNumber(writer, value, 8, false);
      case "float", "float32" -> writer.writeInteger(Float.floatToRawIntBits(value.getAsFloat()), 4);
      case "double", "float64" -> writer.writeInteger(Double.doubleToRawLongBits(value.getAsDouble()), 8);
      default -> throw new IOException("Unsupported CDR type: " + type);
    }
  }

  private static void writeNumber(CdrWriter writer, JsonElement value, int width, boolean signed) throws IOException {
    BigInteger number;
    try {
      number = new BigInteger(value.getAsString());
    } catch (NumberFormatException e) {
      throw new IOException("CDR integer requires an integral value", e);
    }
    int bits = width * 8;
    BigInteger minimum = signed ? BigInteger.ONE.shiftLeft(bits - 1).negate() : BigInteger.ZERO;
    BigInteger maximum = BigInteger.ONE.shiftLeft(signed ? bits - 1 : bits).subtract(BigInteger.ONE);
    if (number.compareTo(minimum) < 0 || number.compareTo(maximum) > 0) {
      throw new IOException("CDR integer out of range: " + number);
    }
    writer.writeInteger(number.longValue(), width);
  }


  private static JsonElement readValue(CdrReader reader, CdrField field) throws IOException {
    String type = field.type();
    return switch (type) {
      case "struct" -> readFields(reader, field.children());
      case "array", "sequence" -> {
        long length = type.equals("array") ? field.length() : reader.readInteger(4, false);
        if (length > MAX_ELEMENTS) {
          throw new IOException("CDR collection too large");
        }
        JsonArray array = new JsonArray();
        for (int i = 0; i < length; i++) {
          array.add(readValue(reader, field.element()));
        }
        yield array;
      }
      case "string" -> {
        long length = reader.readInteger(4, false);
        if (length < 1 || length > reader.remaining()) {
          throw new IOException("Invalid CDR string length");
        }
        byte[] bytes = reader.readBytes((int) length);
        if (bytes[bytes.length - 1] != 0) {
          throw new IOException("CDR string is not NUL terminated");
        }
        yield new JsonPrimitive(new String(bytes, 0, bytes.length - 1, StandardCharsets.UTF_8));
      }
      case "boolean", "bool" -> {
        long number = reader.readInteger(1, false);
        if (number > 1) {
          throw new IOException("Invalid CDR boolean");
        }
        yield new JsonPrimitive(number == 1);
      }
      case "char" -> new JsonPrimitive(Character.toString((char) reader.readInteger(1, false)));
      case "octet", "uint8" -> new JsonPrimitive(reader.readInteger(1, false));
      case "int8" -> new JsonPrimitive(reader.readInteger(1, true));
      case "short", "int16" -> new JsonPrimitive(reader.readInteger(2, true));
      case "ushort", "uint16" -> new JsonPrimitive(reader.readInteger(2, false));
      case "long", "int32" -> new JsonPrimitive(reader.readInteger(4, true));
      case "ulong", "uint32" -> new JsonPrimitive(reader.readInteger(4, false));
      case "longlong", "int64" -> new JsonPrimitive(reader.readInteger(8, true));
      case "ulonglong", "uint64" -> {
        long signedBits = reader.readInteger(8, true);
        BigInteger number = BigInteger.valueOf(signedBits);
        yield new JsonPrimitive(signedBits < 0 ? number.add(TWO_TO_64) : number);
      }
      case "float", "float32" -> new JsonPrimitive(Float.intBitsToFloat((int) reader.readInteger(4, true)));
      case "double", "float64" -> new JsonPrimitive(Double.longBitsToDouble(reader.readInteger(8, true)));
      default -> throw new IOException("Unsupported CDR type: " + type);
    };
  }

}
