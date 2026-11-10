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
package io.mapsmessaging.schemas.formatters.impl.cbc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification;
import io.mapsmessaging.schemas.formatters.impl.Uint64Values;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Restores numeric precision only for unscaled, explicitly declared 64-bit unsigned CBC fields. */
public final class CbcUint64Fields {
  private CbcUint64Fields() {
  }

  public static void restore(JsonObject source, Map<String, Object> values, List<FieldSpecification> fields) throws IOException {
    if (fields == null) {
      return;
    }
    for (FieldSpecification field : fields) {
      if (!source.has(field.getName()) || !values.containsKey(field.getName())) {
        continue;
      }
      JsonElement input = source.get(field.getName());
      if (input == null || input.isJsonNull()) {
        continue;
      }
      if (field.getType().equalsIgnoreCase("uint") && Integer.valueOf(64).equals(field.getSize())
          && field.getEncalc() == null && (field.getEnumTable() == null || field.getEnumTable().isEmpty())) {
        values.put(field.getName(), Uint64Values.fromJson(input));
      } else if (field.getType().equalsIgnoreCase("struct") && field.getFields() != null) {
        Object current = values.get(field.getName());
        if (input.isJsonObject() && current instanceof Map<?, ?> nested) {
          @SuppressWarnings("unchecked")
          Map<String, Object> nestedMap = (Map<String, Object>) nested;
          restore(input.getAsJsonObject(), nestedMap, field.getFields());
        } else if (input.isJsonArray() && current instanceof List<?> list) {
          restoreArray(input.getAsJsonArray(), list, field.getFields());
        }
      }
    }
  }
  private static void restoreArray(JsonArray source, List<?> values, List<FieldSpecification> children) throws IOException {
    @SuppressWarnings("unchecked")
    List<Object> writable = (List<Object>) values;
    for (int i = 0; i < values.size() && i < source.size(); i++) {
      FieldSpecification element;
      if (children.size() == values.size()) {
        element = children.get(i);
      } else if (children.size() == 1) {
        element = children.get(0);
      } else {
        return;
      }
      if (element.getType().equalsIgnoreCase("uint") && Integer.valueOf(64).equals(element.getSize())
          && element.getEncalc() == null && (element.getEnumTable() == null || element.getEnumTable().isEmpty())) {
        writable.set(i, Uint64Values.fromJson(source.get(i)));
      } else if (element.getType().equalsIgnoreCase("struct") && element.getFields() != null
          && source.get(i).isJsonObject() && values.get(i) instanceof Map<?, ?> nested) {
        @SuppressWarnings("unchecked")
        Map<String, Object> nestedMap = (Map<String, Object>) nested;
        restore(source.get(i).getAsJsonObject(), nestedMap, element.getFields());
      }
    }
  }

}
