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

import com.google.gson.JsonObject;

import java.io.IOException;

/** Resolves a canonical CDR layout, inline IDL or an explicitly ordered JSON Schema. */
public final class CdrSchemaResolver {

  private CdrSchemaResolver() {
  }

  public static JsonObject resolve(JsonObject schema) throws IOException {
    try {
      return resolveDefinition(schema);
    } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException e) {
      throw new IOException("Invalid CDR schema definition: " + e.getMessage(), e);
    }
  }

  private static JsonObject resolveDefinition(JsonObject schema) throws IOException {
    if (schema == null) {
      throw new IOException("CDR schema is missing");
    }
    int alternatives = (schema.has("fields") ? 1 : 0) + (schema.has("idl") ? 1 : 0) + (schema.has("properties") ? 1 : 0);
    if (alternatives != 1) {
      throw new IOException("CDR schema must contain exactly one of fields, idl or JSON Schema properties");
    }
    if (schema.has("fields")) {
      if (!schema.get("fields").isJsonArray()) {
        throw new IOException("CDR fields must be an array");
      }
      return schema.deepCopy();
    }
    if (schema.has("idl")) {
      if (!schema.get("idl").isJsonPrimitive() || !schema.get("idl").getAsJsonPrimitive().isString()) {
        throw new IOException("CDR idl must be a string");
      }
      String typeName = schema.has("typeName") ? schema.get("typeName").getAsString() : null;
      JsonObject result = CdrIdlParser.parse(schema.get("idl").getAsString(), typeName);
      if (schema.has("littleEndian")) {
        result.add("littleEndian", schema.get("littleEndian").deepCopy());
      }
      return result;
    }
    return CdrJsonSchemaParser.parse(schema);
  }
}
