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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.schemas.config.SchemaConfig;
import lombok.Getter;

import java.util.Map;

import static io.mapsmessaging.schemas.config.SchemaConfigFactory.gson;

public class MessagePackSchemaConfig extends SimpleSchemaConfig {

  private static final String NAME = "MessagePack";

  @Getter
  private final String schema;

  public MessagePackSchemaConfig() {
    super(NAME);
    schema = "{}";
    setMimeType("application/msgpack");
  }

  public MessagePackSchemaConfig(String schema) {
    super(NAME);
    this.schema = schema;
    setMimeType("application/msgpack");
  }

  private MessagePackSchemaConfig(Map<String, Object> config) {
    super(NAME, config);
    Object obj = config.get("jsonSchema");
    if (obj instanceof Map) {
      @SuppressWarnings("unchecked")
      JsonObject jsonSchema = gson.toJsonTree((Map<String, Object>) obj).getAsJsonObject();
      schema = gson.toJson(jsonSchema);
    } else {
      schema = "{}";
    }
  }

  @Override
  protected void packData(JsonObject jsonObject) {
    super.packData(jsonObject);
    JsonObject schemaObject = JsonParser.parseString(schema).getAsJsonObject();
    jsonObject.add("jsonSchema", schemaObject);
  }

  @Override
  protected SchemaConfig getInstance(Map<String, Object> config) {
    return new MessagePackSchemaConfig(config);
  }
}
