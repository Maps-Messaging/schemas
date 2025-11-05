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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.cbc.CbcFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Compact Binary Codec schema configuration.
 * Type discriminator: "cbc"
 */
@Schema(name = "CbcSchemaConfig", description = "Schema describing a bit-packed Compact Binary Codec layout")
public class CbcSchemaConfig extends SchemaConfig {

  // ---- Constants ----
  public static final String FORMAT = "cbc";
  public static final String DEFAULT_MIME = "application/x-cbc";

  @Getter
  @Setter
  private CbcFormat cbcFormat;

  public CbcSchemaConfig() {
    super(FORMAT);
  }

  public CbcSchemaConfig(SchemaConfig config) {
    super(config);
    if (config.getSchema() != null) {
      cbcFormat = gson.fromJson(config.getSchema(), CbcFormat.class);
    }

  }

  public static List<CbcSchemaConfig> parseSchema(String config) {
    JsonObject jsonSchema = JsonParser.parseString(config).getAsJsonObject();
    List<CbcSchemaConfig> list = new ArrayList<>();
    JsonArray jsonArray = jsonSchema.getAsJsonArray("messages");
    for (int i = 0; i < jsonArray.size(); i++) {
      JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
      CbcSchemaConfig cbcSchemaConfig = new CbcSchemaConfig();
      cbcSchemaConfig.setSchema(jsonObject);
      list.add(cbcSchemaConfig);
    }
    return list;
  }

  @Override
  public String getMimeType() {
    return DEFAULT_MIME;
  }

  public void setSchema(String schema) {
    JsonElement element = JsonParser.parseString(schema);
    if (element.isJsonObject()) {
      JsonObject obj = element.getAsJsonObject();
      setSchema(obj);
    }
  }

  public void setSchema(JsonObject schema) {
    cbcFormat = gson.fromJson(schema, CbcFormat.class);
    super.setSchema(schema);
  }

  @Override
  public SchemaConfig getInstance(SchemaConfig config) {
    return new CbcSchemaConfig(config);
  }

  @Override
  public byte[] pack() throws IOException {
    if (cbcFormat != null) {
      if (cbcFormat.getFields() == null || cbcFormat.getFields().isEmpty()) {
        throw new IOException("CBC format fields are required");
      }
    }
    return super.pack();
  }

  public long getMessageKey() {
    JsonObject jsonObject = getSchema();
    if (jsonObject != null && jsonObject.has("messageKey")) {
      return jsonObject.get("messageKey").getAsLong();
    }
    return Long.MAX_VALUE;
  }
}
