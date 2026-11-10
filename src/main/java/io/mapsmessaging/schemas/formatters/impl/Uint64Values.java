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

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/** Exact unsigned 64-bit conversions, invoked only for schema-declared unsigned 64-bit fields. */
public final class Uint64Values {
  public static final BigInteger MAX_VALUE = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
  private static final BigInteger MODULUS = BigInteger.ONE.shiftLeft(64);

  private Uint64Values() {
  }

  public static BigInteger fromSignedBits(long bits) {
    BigInteger value = BigInteger.valueOf(bits);
    return bits < 0 ? value.add(MODULUS) : value;
  }

  public static BigInteger fromJson(JsonElement element) throws IOException {
    if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
      throw new IOException("uint64 requires a JSON integer");
    }
    try {
      return requireRange(new BigInteger(element.getAsString()));
    } catch (NumberFormatException exception) {
      throw new IOException("uint64 requires an integral decimal value", exception);
    }
  }

  public static BigInteger fromValue(Object value) throws IOException {
    if (value instanceof BigInteger integer) {
      return requireRange(integer);
    }
    if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
      return requireRange(BigInteger.valueOf(((Number) value).longValue()));
    }
    throw new IOException("uint64 requires an exact integral numeric value");
  }

  public static BigInteger requireRange(BigInteger value) throws IOException {
    if (value.signum() < 0 || value.compareTo(MAX_VALUE) > 0) {
      throw new IOException("uint64 out of range: " + value);
    }
    return value;
  }

  /**
   * Restore exact unsigned values in an existing untyped map, leaving every other field unchanged.
   * Schema metadata must explicitly declare format or x-cdr-type as uint64.
   */
  public static void applyJsonSchema(JsonObject definition, JsonObject values, Map<String, Object> target) throws IOException {
    if (definition == null || values == null || !definition.has("properties") || !definition.get("properties").isJsonObject()) {
      return;
    }
    JsonObject properties = definition.getAsJsonObject("properties");
    for (Map.Entry<String, JsonElement> entry : properties.entrySet()) {
      String name = entry.getKey();
      if (!values.has(name) || !target.containsKey(name) || !entry.getValue().isJsonObject()) {
        continue;
      }
      JsonElement source = values.get(name);
      JsonObject field = entry.getValue().getAsJsonObject();
      if (isUint64(field)) {
        target.put(name, fromJson(source));
      } else if (source.isJsonObject() && target.get(name) instanceof Map<?, ?> nested) {
        @SuppressWarnings("unchecked")
        Map<String, Object> nestedMap = (Map<String, Object>) nested;
        applyJsonSchema(field, source.getAsJsonObject(), nestedMap);
      } else if (source.isJsonArray() && target.get(name) instanceof List<?> list && field.has("items")
          && field.get("items").isJsonObject()) {
        applyArray(field.getAsJsonObject("items"), source.getAsJsonArray(), list);
      }
    }
  }

  private static void applyArray(JsonObject definition, JsonArray values, List<?> target) throws IOException {
    int length = Math.min(values.size(), target.size());
    for (int i = 0; i < length; i++) {
      JsonElement source = values.get(i);
      if (isUint64(definition)) {
        @SuppressWarnings("unchecked")
        List<Object> writable = (List<Object>) target;
        writable.set(i, fromJson(source));
      } else if (source.isJsonObject() && target.get(i) instanceof Map<?, ?> nested) {
        @SuppressWarnings("unchecked")
        Map<String, Object> nestedMap = (Map<String, Object>) nested;
        applyJsonSchema(definition, source.getAsJsonObject(), nestedMap);
      } else if (source.isJsonArray() && target.get(i) instanceof List<?> children && definition.has("items")
          && definition.get("items").isJsonObject()) {
        applyArray(definition.getAsJsonObject("items"), source.getAsJsonArray(), children);
      }
    }
  }

  private static boolean isUint64(JsonObject field) {
    return (field.has("format") && "uint64".equals(field.get("format").getAsString()))
        || (field.has("x-cdr-type") && "uint64".equals(field.get("x-cdr-type").getAsString()));
  }
}
