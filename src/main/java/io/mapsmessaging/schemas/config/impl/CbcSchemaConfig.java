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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.cbc.CrcType;
import io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
  private boolean littleEndian = true;

  @Getter
  @Setter
  private boolean includeHeaderChecksum = false;

  @Getter
  @Setter
  private CrcType checksumType = CrcType.NONE;

  @Getter
  @Setter
  private int messageTypeId = 0;

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
    if (config.containsKey("littleEndian")) {
      Object value = config.get("littleEndian");
      littleEndian = (value instanceof Boolean) ? (Boolean) value : Boolean.parseBoolean(String.valueOf(value));
    }
    if (config.containsKey("includeHeaderChecksum")) {
      Object value = config.get("includeHeaderChecksum");
      includeHeaderChecksum = (value instanceof Boolean) ? (Boolean) value : Boolean.parseBoolean(String.valueOf(value));
    }
    if (config.containsKey("checksumType")) {
      Object value = config.get("checksumType");
      try {
        checksumType = CrcType.valueOf(String.valueOf(value));
      } catch (IllegalArgumentException ignored) {
        checksumType = CrcType.NONE;
      }
    }
    if (config.containsKey("messageTypeId")) {
      Object value = config.get("messageTypeId");
      try {
        messageTypeId = Integer.parseInt(String.valueOf(value));
      } catch (NumberFormatException ignored) {
        messageTypeId = 0;
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

  @Override
  public byte[] getSchemaDefinition() {
    // Return the schema layout as JSON bytes for portability
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("type", NAME);
    jsonObject.addProperty("littleEndian", littleEndian);
    jsonObject.addProperty("includeHeaderChecksum", includeHeaderChecksum);
    jsonObject.addProperty("checksumType", checksumType.name());
    jsonObject.addProperty("messageTypeId", messageTypeId);
    jsonObject.add("fields", toFieldsJsonArray());
    return jsonObject.toString().getBytes(StandardCharsets.UTF_8);
  }

  @Override
  protected JsonObject packData() throws IOException {
    JsonObject schemaJson = new JsonObject();
    super.packData(schemaJson);
    schemaJson.addProperty(MIME_TYPE, getMimeType());
    schemaJson.addProperty("littleEndian", littleEndian);
    schemaJson.addProperty("includeHeaderChecksum", includeHeaderChecksum);
    schemaJson.addProperty("checksumType", checksumType.name());
    schemaJson.addProperty("messageTypeId", messageTypeId);
    schemaJson.add("fields", toFieldsJsonArray());
    // Base class will wrap this under { "schema": ... } and append common headers
    return schemaJson;
  }

  @Override
  protected SchemaConfig getInstance(Map<String, Object> config) {
    return new CbcSchemaConfig(config);
  }

  private JsonArray toFieldsJsonArray() {
    JsonArray array = new JsonArray();
    for (FieldSpecification fieldSpecification : fieldSpecificationList) {
      array.add(fieldSpecification.toJson());
    }
    return array;
  }
}
