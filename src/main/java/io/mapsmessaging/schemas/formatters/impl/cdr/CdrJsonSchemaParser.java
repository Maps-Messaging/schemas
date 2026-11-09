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
import java.util.HashSet;
import java.util.Set;

/**
 * Converts a JSON Schema with explicit CDR type/order metadata into the canonical ordered field descriptor format.
 * A general JSON Schema cannot determine CDR numeric widths or wire ordering without metadata.
 */
public final class CdrJsonSchemaParser {

  private static final int MAX_DEPTH = 64;

  private CdrJsonSchemaParser() {
  }

  public static JsonObject parse(JsonObject schema) throws IOException {
    if (schema == null) {
      throw new IOException("CDR JSON Schema is missing");
    }
    JsonObject result = new JsonObject();
    if (schema.has("littleEndian")) {
      result.add("littleEndian", schema.get("littleEndian").deepCopy());
    }
    result.add("fields", parseProperties(schema, 0));
    return result;
  }

  private static JsonArray parseProperties(JsonObject object, int depth) throws IOException {
    if (depth > MAX_DEPTH) {
      throw new IOException("CDR JSON Schema nesting limit exceeded");
    }
    if (!hasString(object, "type", "object") || !object.has("properties") || !object.get("properties").isJsonObject()) {
      throw new IOException("CDR JSON Schema requires an object with properties");
    }
    if (!object.has("x-cdr-order") || !object.get("x-cdr-order").isJsonArray()) {
      throw new IOException("CDR JSON Schema requires x-cdr-order to define field order");
    }
    JsonObject properties = object.getAsJsonObject("properties");
    JsonArray order = object.getAsJsonArray("x-cdr-order");
    if (order.size() != properties.size()) {
      throw new IOException("x-cdr-order must list every property exactly once");
    }
    Set<String> seen = new HashSet<>();
    JsonArray fields = new JsonArray();
    for (JsonElement entry : order) {
      if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
        throw new IOException("x-cdr-order requires field names");
      }
      String name = entry.getAsString();
      if (!properties.has(name) || !seen.add(name) || !properties.get(name).isJsonObject()) {
        throw new IOException("Invalid or duplicate x-cdr-order member: " + name);
      }
      JsonObject type = parseType(properties.getAsJsonObject(name), depth + 1);
      type.addProperty("name", name);
      fields.add(type);
    }
    return fields;
  }

  private static JsonObject parseType(JsonObject schema, int depth) throws IOException {
    if (depth > MAX_DEPTH) {
      throw new IOException("CDR JSON Schema nesting limit exceeded");
    }
    if (!schema.has("type") || !schema.get("type").isJsonPrimitive()) {
      throw new IOException("CDR JSON Schema field requires a single type");
    }
    String type = schema.get("type").getAsString();
    JsonObject result = new JsonObject();
    switch (type) {
      case "object" -> {
        result.addProperty("type", "struct");
        result.add("fields", parseProperties(schema, depth + 1));
      }
      case "array" -> {
        if (!schema.has("items") || !schema.get("items").isJsonObject()) {
          throw new IOException("CDR JSON Schema arrays require a single items schema");
        }
        result.add("element", parseType(schema.getAsJsonObject("items"), depth + 1));
        boolean fixed = schema.has("minItems") && schema.has("maxItems")
            && schema.get("minItems").getAsInt() == schema.get("maxItems").getAsInt();
        result.addProperty("type", fixed ? "array" : "sequence");
        if (fixed) {
          result.add("length", schema.get("minItems").deepCopy());
        } else if (schema.has("minItems") || schema.has("maxItems")) {
          throw new IOException("Bounded CDR sequences require explicit codec support");
        }
      }
      case "integer", "number", "string", "boolean" -> {
        String format = schema.has("x-cdr-type") ? schema.get("x-cdr-type").getAsString()
            : schema.has("format") ? schema.get("format").getAsString() : "";
        if (type.equals("boolean")) {
          if (!format.isEmpty() && !format.equals("boolean")) {
            throw new IOException("Conflicting CDR boolean format " + format);
          }
          result.addProperty("type", "boolean");
        } else if (type.equals("string") && format.isEmpty()) {
          result.addProperty("type", "string");
        } else if (type.equals("string") && format.equals("char")) {
          result.addProperty("type", "char");
        } else {
          boolean valid = switch (type) {
            case "integer" -> Set.of("int8", "uint8", "int16", "uint16", "int32", "uint32", "int64", "uint64", "octet").contains(format);
            case "number" -> Set.of("float32", "float64").contains(format);
            default -> false;
          };
          if (!valid) {
            throw new IOException("CDR JSON Schema requires an exact compatible numeric or string format for " + type);
          }
          result.addProperty("type", format);
        }
      }
      default -> throw new IOException("Unsupported CDR JSON Schema field type: " + type);
    }
    return result;
  }

  private static boolean hasString(JsonObject object, String key, String expected) {
    return object.has(key) && object.get(key).isJsonPrimitive() && expected.equals(object.get(key).getAsString());
  }
}
