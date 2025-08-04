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

package io.mapsmessaging.schemas.formatters.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.JsonSchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.walker.MapResolver;
import io.mapsmessaging.schemas.formatters.walker.StructuredResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.FORMATTER_UNEXPECTED_OBJECT;
import static io.mapsmessaging.schemas.logging.SchemaLogMessages.JSON_PARSE_EXCEPTION;

/**
 * The type Json formatter.
 */
public class JsonFormatter extends MessageFormatter {

  private final JsonNode schemaNode;
  private final JsonSchema schema;
  private final JsonSchemaFactory schemaFactory;

  /**
   * Instantiates a new Json formatter.
   */
  public JsonFormatter() {
    schemaNode = null;
    schema = null;
    schemaFactory = null;
  }


  public JsonFormatter(String schemaString) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();

    // Convert byte[] schema to JsonNode
    schemaNode = objectMapper.readTree(schemaString);

    // Create JsonSchema instance
    schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    schema = schemaFactory.getSchema(schemaNode);
  }

  @Override
  public ParsedObject parse(byte[] payload) {
    try {
      String jsonString = new String(payload, StandardCharsets.UTF_8);
      JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();

      if (schema != null) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonString);
        Set<ValidationMessage> validationResult = schema.validate(jsonNode);
        if (!validationResult.isEmpty()) {
          logger.log(JSON_PARSE_EXCEPTION, getName(), validationResult);
          return new DefaultParser(payload);
        }
      }

      Gson gson = new Gson();
      @SuppressWarnings("unchecked")
      Map<String, Object> map = gson.fromJson(json, Map.class);
      return new StructuredResolver(new MapResolver(map), json);
    } catch (Exception e) {
      logger.log(FORMATTER_UNEXPECTED_OBJECT, getName(), payload);
      return new DefaultParser(payload);
    }
  }

  @Override
  public Map<String, Object> getFormat() {
    if (schemaNode == null || !schemaNode.has("properties")) {
      return Map.of();
    }

    try {
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode propertiesNode = schemaNode.get("properties");
      Map<String, Object> result = new java.util.LinkedHashMap<>();

      propertiesNode.fields().forEachRemaining(entry -> {
        String fieldName = entry.getKey();
        JsonNode attributes = entry.getValue();
        Map<String, Object> attrMap = objectMapper.convertValue(attributes, Map.class);
        result.put(fieldName, attrMap);
      });

      return result;
    } catch (Exception e) {
      logger.log(FORMATTER_UNEXPECTED_OBJECT, getName(), e.getMessage());
      return Map.of();
    }
  }
  @Override
  public JsonObject parseToJson(byte[] payload) throws IOException {
    return JsonParser.parseString(new String(payload, StandardCharsets.UTF_8)).getAsJsonObject();
  }


  @Override
  public MessageFormatter getInstance(SchemaConfig config) throws IOException {
    JsonSchemaConfig jsonSchemaConfig = (JsonSchemaConfig) config;
    return new JsonFormatter(jsonSchemaConfig.getSchema());
  }

  @Override
  public String getName() {
    return "JSON";
  }

}
