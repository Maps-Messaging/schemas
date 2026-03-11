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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * The type Json schema config.
 */
@Schema(description = "JSON Schema Configuration")
public class JsonSchemaConfig extends SchemaConfig {

  private static final String EMPTY_SCHEMA = "{}";
  private static final String NAME = "json";

  private List<SchemaConfig> bundledSchemas = null;

  /**
   * Instantiates a new Json schema config.
   */
  public JsonSchemaConfig() {
    super(NAME);
    setSchema(JsonParser.parseString(EMPTY_SCHEMA).getAsJsonObject());
  }

  public JsonSchemaConfig(String schema) {
    super(NAME);
    setSchema(JsonParser.parseString(schema).getAsJsonObject());
  }


  public JsonSchemaConfig(Path schemaDirectory) throws IOException {
    super(NAME);
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode schemaNode = objectMapper.readTree(schemaDirectory.toFile());
    JsonObject jsonObject = JsonParser.parseString(schemaNode.toString()).getAsJsonObject();
    setSchema(jsonObject);
    setUniqueId(UUID.nameUUIDFromBytes(schemaNode.toString().getBytes()));
    setName(schemaDirectory.getFileName().toString());
    setSource(schemaDirectory.toUri().toString());
    setVersion(1);
  }

  @Override
  public boolean isBundle() {
    JsonObject schema = getSchema();
    if (schema == null || !schema.has("$defs")) {
      return false;
    }

    JsonElement defs = schema.get("$defs");
    return defs != null &&
        defs.isJsonObject() &&
        !defs.getAsJsonObject().entrySet().isEmpty();
  }

  @Override
  public synchronized List<SchemaConfig> getBundledSchemas() {
    if (bundledSchemas == null) {
      bundledSchemas = new ArrayList<>();
      JsonObject definitions = getSchema().getAsJsonObject("$defs");
      for (Map.Entry<String, JsonElement> entry : definitions.entrySet()) {
        String definitionName = entry.getKey();
        String name = (getUniqueId() + "::" + definitionName);
        JsonObject definitionObject = entry.getValue().getAsJsonObject();
        JsonSchemaConfig schema = new JsonSchemaConfig();
        schema.setUniqueId(UUID.nameUUIDFromBytes(name.getBytes()));
        schema.setParentUuid(getUniqueId());
        schema.setName(name);
        schema.setSource("#/$defs/" + escapeJsonPointerToken(definitionName));
        schema.setVersion(getVersion());
        schema.setSchema(definitionObject);
        bundledSchemas.add(schema);
      }
      bundledSchemas = Collections.unmodifiableList(bundledSchemas);
    }
    return bundledSchemas;
  }

  private JsonSchemaConfig(SchemaConfig config) {
    super(config);
  }

  @Override
  public String getMimeType() {
    return "application/json";
  }

  public SchemaConfig getInstance(SchemaConfig config) {
    return new JsonSchemaConfig(config);
  }

  private String escapeJsonPointerToken(String value) {
    return value.replace("~", "~0").replace("/", "~1");
  }
}
