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
package io.mapsmessaging.schemas.formatters.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParseException;
import io.mapsmessaging.schemas.formatters.ParseMode;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.walker.MapResolver;
import io.mapsmessaging.schemas.formatters.walker.StructuredResolver;
import io.mapsmessaging.schemas.repository.SchemaResolver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Plain XCDR1 (CDR_BE / CDR_LE) DDS sample formatter.
 *
 * <p>The schema has an ordered "fields" array. Each field has a "name" and
 * "type". Struct fields contain another "fields" array; array and sequence
 * fields contain an "element" descriptor (a type string or a descriptor
 * object). Fixed arrays additionally contain a "length". This formatter
 * deliberately does not interpret IDL, XCDR2, parameter-list CDR or unions.
 */
public class CdrFormatter extends MessageFormatter {

  private static final BigInteger TWO_TO_64 = BigInteger.ONE.shiftLeft(64);
  private static final int MAX_ELEMENTS = 1_000_000;

  private final JsonObject definition;
  private final boolean littleEndian;
  private final List<Field> fields;

  public CdrFormatter() {
    definition = null;
    littleEndian = true;
    fields = List.of();
  }

  private CdrFormatter(JsonObject schema) throws IOException {
    if (schema == null || !schema.has("fields") || !schema.get("fields").isJsonArray()) {
      throw new IOException("CDR schema requires an ordered fields array");
    }
    definition = schema.deepCopy();
    littleEndian = !definition.has("littleEndian") || definition.get("littleEndian").getAsBoolean();
    fields = compileFields(definition.getAsJsonArray("fields"));
  }

  @Override
  public String getName() {
    return "CDR";
  }

  @Override
  public MessageFormatter getInstance(SchemaConfig config, SchemaResolver schemaResolver) throws IOException {
    return new CdrFormatter(config.getSchema());
  }

  @Override
  public byte[] parseFromJson(JsonObject jsonObject) throws IOException {
    requireDefinition();
    if (jsonObject == null) {
      throw new IOException("CDR sample cannot be null");
    }
    try {
      Writer writer = new Writer(littleEndian);
      writeFields(writer, fields, jsonObject);
      return writer.toByteArray();
    } catch (IllegalArgumentException | IllegalStateException e) {
      throw new IOException("Unable to encode CDR sample: " + e.getMessage(), e);
    }
  }

  @Override
  public JsonObject parseToJson(byte[] payload, ParseMode parseMode) throws ParseException {
    try {
      requireDefinition();
      if (payload == null || payload.length < 4 || payload[0] != 0 ||
          (payload[1] != 0 && payload[1] != 1) || payload[2] != 0 || payload[3] != 0) {
        throw new IOException("Unsupported or missing plain CDR encapsulation header");
      }
      boolean inputLittleEndian = payload[1] == 1;
      Reader reader = new Reader(payload, inputLittleEndian);
      JsonObject result = readFields(reader, fields);
      if (reader.remaining() != 0) {
        throw new IOException("Trailing bytes in CDR sample");
      }
      return result;
    } catch (IOException | IllegalArgumentException | IllegalStateException e) {
      throw new ParseException("Unable to decode CDR sample: " + e.getMessage(), e);
    }
  }

  @Override
  public ParsedObject parse(byte[] payload, ParseMode parseMode) throws ParseException {
    try {
      JsonObject object = parseToJson(payload, parseMode);
      @SuppressWarnings("unchecked")
      Map<String, Object> map = gson.fromJson(object, Map.class);
      return new StructuredResolver(new MapResolver(map), object);
    } catch (ParseException e) {
      if (parseMode == ParseMode.IGNORE) {
        return new DefaultParser(payload);
      }
      throw e;
    }
  }

  @Override
  public Map<String, Object> getFormat() {
    if (definition == null) {
      return Map.of();
    }
    Map<String, Object> result = new LinkedHashMap<>();
    for (JsonElement field : definition.getAsJsonArray("fields")) {
      JsonObject descriptor = field.getAsJsonObject();
      result.put(descriptor.get("name").getAsString(),
          gson.fromJson(descriptor, Map.class));
    }
    return result;
  }

  private void requireDefinition() throws IOException {
    if (definition == null) {
      throw new IOException("CDR formatter has no schema");
    }
  }


  private record Field(String name, String type, List<Field> children, Field element, int length) {
  }

  private static List<Field> compileFields(JsonArray descriptors) throws IOException {
    List<Field> compiled = new ArrayList<>();
    for (JsonElement entry : descriptors) {
      if (!entry.isJsonObject()) {
        throw new IOException("CDR fields must be objects");
      }
      JsonObject field = entry.getAsJsonObject();
      if (!field.has("name") || !field.get("name").isJsonPrimitive()
          || field.get("name").getAsString().isBlank()) {
        throw new IOException("CDR field requires a name");
      }
      compiled.add(compileField(field, field.get("name").getAsString()));
    }
    return List.copyOf(compiled);
  }

  private static Field compileField(JsonObject field, String name) throws IOException {
    if (!field.has("type")) {
      throw new IOException("CDR field requires a type");
    }
    String type = field.get("type").getAsString().toLowerCase(Locale.ROOT);
    return switch (type) {
      case "struct" -> {
        if (!field.has("fields") || !field.get("fields").isJsonArray()) {
          throw new IOException("CDR struct requires fields");
        }
        yield new Field(name, type, compileFields(field.getAsJsonArray("fields")), null, 0);
      }
      case "array", "sequence" -> {
        if (!field.has("element")) {
          throw new IOException("CDR collection requires element");
        }
        Field element = compileField(descriptor(field.get("element")), null);
        yield new Field(name, type, List.of(), element,
            type.equals("array") ? arrayLength(field) : 0);
      }
      case "boolean", "bool", "char", "octet", "int8", "uint8",
           "short", "ushort", "int16", "uint16", "long", "ulong", "int32", "uint32",
           "longlong", "ulonglong", "int64", "uint64", "float", "float32",
           "double", "float64", "string" -> new Field(name, type, List.of(), null, 0);
      default -> throw new IOException("Unsupported CDR type: " + type);
    };
  }

  private static JsonObject descriptor(JsonElement element) throws IOException {
    if (element.isJsonObject()) {
      return element.getAsJsonObject();
    }
    if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
      JsonObject result = new JsonObject();
      result.add("type", element.deepCopy());
      return result;
    }
    throw new IOException("Invalid CDR element descriptor");
  }

  private static int arrayLength(JsonObject descriptor) throws IOException {
    if (!descriptor.has("length")) {
      throw new IOException("CDR array requires length");
    }
    try {
      int length = descriptor.get("length").getAsInt();
      if (length < 0 || length > MAX_ELEMENTS) {
        throw new IOException("CDR array length out of range");
      }
      return length;
    } catch (NumberFormatException e) {
      throw new IOException("Invalid CDR array length", e);
    }
  }


  private static void writeFields(Writer writer, List<Field> fields, JsonObject values) throws IOException {
    for (Field field : fields) {
      String name = field.name();
      if (!values.has(name) || values.get(name).isJsonNull()) {
        throw new IOException("Missing CDR field: " + name);
      }
      writeValue(writer, field, values.get(name));
    }
  }

  private static JsonObject readFields(Reader reader, List<Field> fields) throws IOException {
    JsonObject values = new JsonObject();
    for (Field field : fields) {
      values.add(field.name(), readValue(reader, field));
    }
    return values;
  }

  private static void writeValue(Writer writer, Field field, JsonElement value) throws IOException {
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

  private static void writeNumber(Writer writer, JsonElement value, int width, boolean signed) throws IOException {
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


  private static JsonElement readValue(Reader reader, Field field) throws IOException {
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

  private static final class Writer {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final boolean littleEndian;

    private Writer(boolean littleEndian) {
      this.littleEndian = littleEndian;
      output.write(0);
      output.write(littleEndian ? 1 : 0);
      output.write(0);
      output.write(0);
    }

    private void align(int width) {
      while ((output.size() - 4) % width != 0) {
        output.write(0);
      }
    }

    private void writeInteger(long value, int width) {
      align(width);
      for (int i = 0; i < width; i++) {
        int shift = (littleEndian ? i : width - i - 1) * 8;
        output.write((int) (value >>> shift) & 0xff);
      }
    }

    private void writeBytes(byte[] data) {
      output.writeBytes(data);
    }

    private byte[] toByteArray() {
      return output.toByteArray();
    }
  }

  private static final class Reader {
    private final ByteBuffer buffer;
    private final boolean littleEndian;

    private Reader(byte[] payload, boolean littleEndian) {
      this.buffer = ByteBuffer.wrap(payload);
      this.buffer.position(4);
      this.littleEndian = littleEndian;
    }

    private int remaining() {
      return buffer.remaining();
    }

    private void align(int width) throws IOException {
      int padding = (width - ((buffer.position() - 4) % width)) % width;
      if (padding > buffer.remaining()) {
        throw new IOException("Truncated CDR padding");
      }
      buffer.position(buffer.position() + padding);
    }

    private long readInteger(int width, boolean signed) throws IOException {
      align(width);
      if (buffer.remaining() < width) {
        throw new IOException("Truncated CDR value");
      }
      long result = 0;
      for (int i = 0; i < width; i++) {
        long octet = buffer.get() & 0xffL;
        result |= octet << ((littleEndian ? i : width - i - 1) * 8);
      }
      if (signed && width < 8) {
        int shift = 64 - width * 8;
        return (result << shift) >> shift;
      }
      return result;
    }

    private byte[] readBytes(int count) throws IOException {
      if (count < 0 || count > buffer.remaining()) {
        throw new IOException("Truncated CDR data");
      }
      byte[] result = new byte[count];
      buffer.get(result);
      return result;
    }
  }
}
