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

package io.mapsmessaging.schemas.config.impl;

import com.google.gson.*;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static io.mapsmessaging.schemas.config.Constants.SCHEMA;

/**
 * Compact Binary Codec schema configuration.
 * Type discriminator: "cbc"
 */
@Schema(name = "CbcSchemaConfig", description = "Schema describing a bit-packed Compact Binary Codec layout")
public class CbcSchemaConfig extends SchemaConfig {

  // ---- Constants ----
  public static final String SCHEMA_NAME = "cbc";
  public static final String DEFAULT_MIME = "application/x-cbc";

  private static final String KEY_DIRECTION = "direction";
  private static final String KEY_NAME = "name";
  private static final String KEY_DESCRIPTION = "description";
  private static final String KEY_MESSAGE_KEY = "messageKey";
  private static final String KEY_FIELDS = "fields";

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
    super(SCHEMA_NAME);
    setMimeType(DEFAULT_MIME);
  }

  public CbcSchemaConfig(Map<String, Object> config) {
    super(SCHEMA_NAME, config);
    if (getMimeType() == null || getMimeType().isEmpty()) {
      setMimeType(DEFAULT_MIME);
    }
    Map<String, Object> map = (Map<String, Object>) config.get(SCHEMA);
    parseSchema(map);
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
    JsonObject jsonSchema = JsonParser.parseString(schema).getAsJsonObject();
    parseSchema(toObjectMap(jsonSchema.asMap()));
  }

  protected void processJsonSchema(JsonObject jsonSchema) {
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
    packData(schemaJson);
    JsonObject schema = new JsonObject();
    schema.addProperty(KEY_DIRECTION, direction);
    schema.addProperty(KEY_NAME, name);
    schema.addProperty(KEY_DESCRIPTION, description);
    schema.addProperty(KEY_MESSAGE_KEY, messageKey);
    schema.add(KEY_FIELDS, toFieldsJsonArray());
    schemaJson.add(SCHEMA, schema);
    return schemaJson;
  }

  @Override
  protected SchemaConfig getInstance(Map<String, Object> config) {
    return new CbcSchemaConfig(config);
  }

  private void parseSchema(Map<String, Object> config) {
    if (config.containsKey(KEY_DIRECTION)) {
      Object value = config.get(KEY_DIRECTION);
      direction = String.valueOf(value);
    }
    if (config.containsKey(KEY_NAME)) {
      Object value = config.get(KEY_NAME);
      name = String.valueOf(value);
    }
    if (config.containsKey(KEY_DESCRIPTION)) {
      Object value = config.get(KEY_DESCRIPTION);
      description = String.valueOf(value);
    }
    if (config.containsKey(KEY_MESSAGE_KEY)) {
      Object value = config.get(KEY_MESSAGE_KEY);
      try {
        if (value instanceof Double d) {
          messageKey = d.intValue();
        } else {
          messageKey = Integer.parseInt(String.valueOf(value));
        }
      } catch (NumberFormatException ignored) {
        messageKey = 0;
      }
    }
    if (config.containsKey(KEY_FIELDS)) {
      Object value = config.get(KEY_FIELDS);
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

  public static List<CbcSchemaConfig> parseSchema(String config) {
    JsonObject jsonSchema = JsonParser.parseString(config).getAsJsonObject();
    List<CbcSchemaConfig> list = new ArrayList<>();
    JsonArray jsonArray = jsonSchema.getAsJsonArray("messages");
    for (int i = 0; i < jsonArray.size(); i++) {
      JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
      CbcSchemaConfig cbcSchemaConfig = new CbcSchemaConfig();
      cbcSchemaConfig.processJsonSchema(jsonObject);
      cbcSchemaConfig.setUniqueId(UUID.randomUUID().toString());
      list.add(cbcSchemaConfig);
    }
    return list;
  }
}
