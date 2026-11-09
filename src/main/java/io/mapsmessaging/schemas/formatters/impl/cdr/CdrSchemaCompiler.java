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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CdrSchemaCompiler {
  private static final int MAX_ELEMENTS = 1_000_000;

  private CdrSchemaCompiler() {
  }

  public static List<CdrField> compileFields(JsonArray descriptors) throws IOException {
    List<CdrField> compiled = new ArrayList<>();
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

  private static CdrField compileField(JsonObject field, String name) throws IOException {
    if (!field.has("type")) {
      throw new IOException("CDR field requires a type");
    }
    String type = field.get("type").getAsString().toLowerCase(Locale.ROOT);
    return switch (type) {
      case "struct" -> {
        if (!field.has("fields") || !field.get("fields").isJsonArray()) {
          throw new IOException("CDR struct requires fields");
        }
        yield new CdrField(name, type, compileFields(field.getAsJsonArray("fields")), null, 0);
      }
      case "array", "sequence" -> {
        if (!field.has("element")) {
          throw new IOException("CDR collection requires element");
        }
        CdrField element = compileField(descriptor(field.get("element")), null);
        yield new CdrField(name, type, List.of(), element,
            type.equals("array") ? arrayLength(field) : 0);
      }
      case "boolean", "bool", "char", "octet", "int8", "uint8",
           "short", "ushort", "int16", "uint16", "long", "ulong", "int32", "uint32",
           "longlong", "ulonglong", "int64", "uint64", "float", "float32",
           "double", "float64", "string" -> new CdrField(name, type, List.of(), null, 0);
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
}
