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

package io.mapsmessaging.schemas.config.impl.cbc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.*;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FieldSpecification {

  private String name;
  private String type;
  @Builder.Default
  private Integer size = null;            // nullable; not set for struct
  @Builder.Default
  private Boolean optional = false;
  @Builder.Default
  private Boolean fixed = true;
  @Builder.Default
  private Map<Integer, String> enumTable = null; // CBC "enum"
  @Builder.Default
  private List<FieldSpecification> fields = null; // for struct/array
  @Builder.Default
  private String description = null;
  @Builder.Default
  private String decalc = null;
  @Builder.Default
  private String encalc = null;
  @Builder.Default
  private Long min = null;
  @Builder.Default
  private Long max = null;

  // ---------- JSON (CBC) SERIALIZATION ----------

  @SuppressWarnings("unchecked")
  public static FieldSpecification from(Map<String, Object> map) {
    FieldSpecificationBuilder b = FieldSpecification.builder();

    // Required
    b.name(reqString(map, "name"));
    b.type(reqString(map, "type"));

    // Conditional/optional
    Integer size = optInt(map.get("size"));
    if (size != null) b.size(size);

    Boolean optional = optBool(map.get("optional"));
    if (optional != null) b.optional(optional);

    Boolean fixed = optBool(map.get("fixed"));
    if (fixed != null) b.fixed(fixed);

    String desc = optString(map.get("description"));
    if (desc != null) b.description(desc);

    String decalc = optString(map.get("decalc"));
    if (decalc != null) b.decalc(decalc);

    String encalc = optString(map.get("encalc"));
    if (encalc != null) b.encalc(encalc);

    Long min = optLong(map.get("min"));
    if (min != null) b.min(min);

    Long max = optLong(map.get("max"));
    if (max != null) b.max(max);

    // enum table
    Object enumObj = map.get("enum");
    if (enumObj instanceof Map<?, ?> m) {
      Map<Integer, String> table = new LinkedHashMap<>();
      for (Map.Entry<?, ?> e : m.entrySet()) {
        Integer k = optInt(e.getKey());
        String v = optString(e.getValue());
        if (k != null && v != null) table.put(k, v);
      }
      if (!table.isEmpty()) b.enumTable(table);
    }

    // fields (struct/array)
    Object fieldsObj = map.get("fields");
    if (fieldsObj instanceof List<?> list) {
      List<FieldSpecification> children = new ArrayList<>();
      for (Object item : list) {
        if (item instanceof Map<?, ?> child) {
          children.add(from((Map<String, Object>) child));
        }
      }
      if (!children.isEmpty()) b.fields(children);
    }

    return b.build();
  }

  // ---------- JSON (CBC) DESERIALIZATION ----------

  private static String reqString(Map<String, Object> map, String key) {
    String v = optString(map.get(key));
    if (v == null || v.isEmpty()) throw new IllegalArgumentException("Missing required key: " + key);
    return v;
  }

  // ---------- Helpers (STRICT, small) ----------

  private static String optString(Object o) {
    if (o == null) return null;
    if (o instanceof JsonElement je) return je.isJsonNull() ? null : je.getAsString();
    String s = String.valueOf(o).trim();
    return s.isEmpty() ? null : s;
  }

  private static Integer optInt(Object o) {
    if (o == null) return null;
    try {
      if (o instanceof Number n) return n.intValue();
      String s = String.valueOf(o).trim();
      int dot = s.indexOf('.');
      if (dot > 0) s = s.substring(0, dot);
      if (s.startsWith("0x") || s.startsWith("0X")) return Integer.parseUnsignedInt(s.substring(2), 16);
      return Integer.parseInt(s);
    } catch (Exception ignore) {
      return null;
    }
  }

  private static Long optLong(Object o) {
    if (o == null) return null;
    try {
      if (o instanceof Number n) return n.longValue();
      String s = String.valueOf(o).trim();
      if (s.startsWith("0x") || s.startsWith("0X")) return Long.parseUnsignedLong(s.substring(2), 16);
      return Long.parseLong(s);
    } catch (Exception ignore) {
      return null;
    }
  }

  private static Boolean optBool(Object o) {
    if (o == null) return null;
    if (o instanceof Boolean b) return b;
    String s = String.valueOf(o).trim();
    if ("1".equals(s)) return true;
    if ("0".equals(s)) return false;
    return Boolean.parseBoolean(s);
  }

  public JsonObject toJson() {
    JsonObject o = new JsonObject();
    o.addProperty("name", name);
    o.addProperty("type", type);
    if (size != null) o.addProperty("size", size);
    if (Boolean.TRUE.equals(optional)) o.addProperty("optional", true);
    if (!Boolean.TRUE.equals(fixed)) o.addProperty("fixed", false);
    if (description != null) o.addProperty("description", description);
    if (decalc != null) o.addProperty("decalc", decalc);
    if (encalc != null) o.addProperty("encalc", encalc);
    if (min != null) o.addProperty("min", min);
    if (max != null) o.addProperty("max", max);
    if (enumTable != null && !enumTable.isEmpty()) {
      JsonObject e = new JsonObject();
      for (Map.Entry<Integer, String> en : new TreeMap<>(enumTable).entrySet()) {
        e.addProperty(String.valueOf(en.getKey()), en.getValue());
      }
      o.add("enum", e);
    }
    if (fields != null && !fields.isEmpty()) {
      com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
      for (FieldSpecification f : fields) arr.add(f.toJson());
      o.add("fields", arr);
    }
    return o;
  }
}
