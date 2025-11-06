/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.mapsmessaging.schemas.formatters;

import com.google.gson.JsonObject;

public class JsonValidator {

  public static boolean validateJson(JsonObject lhs, JsonObject rhs) {
    return equalsElement(lhs, rhs, 0.4);
  }

  private static boolean equalsElement(com.google.gson.JsonElement a, com.google.gson.JsonElement b, double tolerance) {
    if (a == null || b == null) {
      return a == b;
    }
    if (a.isJsonNull() || b.isJsonNull()) {
      return a.isJsonNull() && b.isJsonNull();
    }
    if (a.isJsonObject() && b.isJsonObject()) {
      return equalsObject(a.getAsJsonObject(), b.getAsJsonObject(), tolerance);
    }
    if (a.isJsonArray() && b.isJsonArray()) {
      return equalsArray(a.getAsJsonArray(), b.getAsJsonArray(), tolerance);
    }
    if (a.isJsonPrimitive() && b.isJsonPrimitive()) {
      return equalsPrimitive(a.getAsJsonPrimitive(), b.getAsJsonPrimitive(), tolerance);
    }
    return false;
  }

  private static boolean equalsObject(com.google.gson.JsonObject a, com.google.gson.JsonObject b, double tolerance) {
    if (a.size() != b.size()) {
      return false;
    }
    for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : a.entrySet()) {
      String key = entry.getKey();
      if (!b.has(key)) {
        return false;
      }
      if (!equalsElement(entry.getValue(), b.get(key), tolerance)) {
        return false;
      }
    }
    return true;
  }

  private static boolean equalsArray(com.google.gson.JsonArray a, com.google.gson.JsonArray b, double tolerance) {
    if (a.size() != b.size()) {
      return false;
    }
    for (int i = 0; i < a.size(); i++) {
      if (!equalsElement(a.get(i), b.get(i), tolerance)) {
        return false;
      }
    }
    return true;
  }

  private static boolean equalsPrimitive(com.google.gson.JsonPrimitive a, com.google.gson.JsonPrimitive b, double tolerance) {
    if (a.isBoolean() || b.isBoolean()) {

      if (a.isBoolean() && b.isBoolean() && a.getAsBoolean() == b.getAsBoolean()) {
        return true;
      }
      boolean abool = a.getAsBoolean();
      boolean bbool = b.getAsBoolean();
      if (a.isString()) {
        abool = Boolean.parseBoolean(b.getAsString());
      }
      if (b.isString()) {
        bbool = Boolean.parseBoolean(b.getAsString());
      }
      if (a.isNumber()) {
        abool = b.getAsInt() != 0;
      }

      if (b.isNumber()) {
        bbool = b.getAsInt() != 0;
      }
      return (abool == bbool);
    }
    if (a.isNumber() && b.isNumber()) {
      java.math.BigDecimal da = toBigDecimal(a);
      java.math.BigDecimal db = toBigDecimal(b);
      if (tolerance <= 0.0) {
        return da.compareTo(db) == 0;
      } else {
        java.math.BigDecimal diff = da.subtract(db).abs();
        return diff.compareTo(java.math.BigDecimal.valueOf(tolerance)) <= 0;
      }
    }
    return a.getAsString().equals(b.getAsString());
  }

  private static java.math.BigDecimal toBigDecimal(com.google.gson.JsonPrimitive p) {
    // Handles ints and floats uniformly; avoids 12.34 vs 12.34000015258789 noise when tolerance > 0
    try {
      return new java.math.BigDecimal(p.getAsString());
    } catch (NumberFormatException ignore) {
      return new java.math.BigDecimal(p.getAsDouble());
    }
  }
}
