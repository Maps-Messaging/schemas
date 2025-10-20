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
// File: src/main/java/io/mapsmessaging/schemas/config/impl/CbcSchemaConfig.java
package io.mapsmessaging.schemas.config.impl;

import com.google.gson.*;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.mapsmessaging.schemas.config.Constants.MIME_TYPE;

/**
 * Compact Binary Codec schema configuration.
 * Type discriminator: "cbc"
 */
@Schema(name = "CbcSchemaConfig", description = "Schema describing a bit-packed Compact Binary Codec layout")
public class CbcSchemaConfig extends SchemaConfig {

  public static final String NAME = "cbc";
  public static final String DEFAULT_MIME = "application/x-cbc";

  @Getter
  @Setter
  private String name;

  @Getter
  @Setter
  private String direction;

  @Getter
  @Setter
  private String description;

  @Getter
  @Setter
  private int messageKey = 0;

  @Getter
  @Setter
  private List<FieldSpecification> fieldSpecificationList = new ArrayList<>();

  public CbcSchemaConfig() {
    super(NAME);
    setMimeType(DEFAULT_MIME);
  }

  public CbcSchemaConfig(Map<String, Object> config) {
    super(NAME, config);
    if (getMimeType() == null || getMimeType().isEmpty()) {
      setMimeType(DEFAULT_MIME);
    }
    parseSchema(config);
  }

  private static Map<String, Object> toObjectMap(Map<String, JsonElement> source) {
    Map<String, Object> out = new LinkedHashMap<>();
    for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
      out.put(entry.getKey(), fromJsonElement(entry.getValue()));
    }
    return out;
  }

  private static Object fromJsonElement(JsonElement e) {
    if (e == null || e.isJsonNull()) return null;
    if (e.isJsonPrimitive()) {
      JsonPrimitive p = e.getAsJsonPrimitive();
      if (p.isBoolean()) return p.getAsBoolean();
      if (p.isNumber()) {
        Number n = p.getAsNumber();
        // choose integer vs double sensibly
        double d = n.doubleValue();
        long l = n.longValue();
        return (d == (double) l) ? l : d;
      }
      return p.getAsString();
    }
    if (e.isJsonArray()) {
      List<Object> list = new ArrayList<>();
      for (JsonElement el : e.getAsJsonArray()) list.add(fromJsonElement(el));
      return list;
    }
    if (e.isJsonObject()) {
      Map<String, Object> map = new LinkedHashMap<>();
      for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) {
        map.put(en.getKey(), fromJsonElement(en.getValue()));
      }
      return map;
    }
    return null;
  }

  public void setSchema(String schema) {
    JsonObject jsonSchema = new JsonParser().parse(schema).getAsJsonObject();
    parseSchema(toObjectMap(jsonSchema.asMap()));
  }

  @Override
  public byte[] getSchemaDefinition() {
    try {
      return packData().toString().getBytes(StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected JsonObject packData() throws IOException {
    JsonObject schemaJson = new JsonObject();
    super.packData(schemaJson);
    schemaJson.addProperty(MIME_TYPE, getMimeType());
    schemaJson.addProperty("direction", direction);
    schemaJson.addProperty("name", name);
    schemaJson.addProperty("description", description);
    schemaJson.addProperty("messageKey", messageKey);
    schemaJson.add("fields", toFieldsJsonArray());
    return schemaJson;
  }

  @Override
  protected SchemaConfig getInstance(Map<String, Object> config) {
    return new CbcSchemaConfig(config);
  }

  private void parseSchema(Map<String, Object> config) {
    if (config.containsKey("direction")) {
      Object value = config.get("direction");
      direction = String.valueOf(value);
    }
    if (config.containsKey("name")) {
      Object value = config.get("name");
      name = String.valueOf(value);
    }
    if (config.containsKey("description")) {
      Object value = config.get("description");
      description = String.valueOf(value);
    }
    if (config.containsKey("messageKey")) {
      Object value = config.get("messageKey");
      try {
        if (value instanceof Double) {
          messageKey = ((Double) value).intValue();
        } else {
          messageKey = Integer.parseInt(String.valueOf(value));
        }
      } catch (NumberFormatException ignored) {
        messageKey = 0;
      }
    }
    if (config.containsKey("fields")) {
      Object value = config.get("fields");
      if (value instanceof List<?> list) {
        for (Object element : list) {
          if (element instanceof Map<?, ?> mapElement) {
            @SuppressWarnings("unchecked")
            Map<String, Object> fieldMap = (Map<String, Object>) mapElement;
            fieldSpecificationList.add(FieldSpecification.from(fieldMap));
          }
        }
      }
    }
  }

  private JsonArray toFieldsJsonArray() {
    JsonArray array = new JsonArray();
    for (FieldSpecification fieldSpecification : fieldSpecificationList) {
      array.add(fieldSpecification.toJson());
    }
    return array;
  }

}
