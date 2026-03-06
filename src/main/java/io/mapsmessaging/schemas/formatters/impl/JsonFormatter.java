/*
 *
 *     Copyright [ 2020 - 2026 ] [Matthew Buckton]
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package io.mapsmessaging.schemas.formatters.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.walker.MapResolver;
import io.mapsmessaging.schemas.formatters.walker.StructuredResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.FORMATTER_UNEXPECTED_OBJECT;
import static io.mapsmessaging.schemas.logging.SchemaLogMessages.JSON_PARSE_EXCEPTION;

/**
 * The type Json formatter.
 */
public class JsonFormatter extends MessageFormatter {

  private final JsonNode schemaNode;
  private final Schema schema;

  /**
   * Instantiates a new Json formatter.
   */
  public JsonFormatter() {
    schemaNode = null;
    schema = null;
  }

  public JsonFormatter(String schemaString) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    schemaNode = objectMapper.readTree(schemaString);
    String schemaDialect = schemaNode.path("$schema").asText(null);
    SpecificationVersion version = SpecificationVersion.fromDialectId(schemaDialect).orElse(SpecificationVersion.DRAFT_7);
    SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(version);
    schema = schemaRegistry.getSchema(schemaNode);
  }

  @Override
  public ParsedObject parse(byte[] payload) {
    try {
      String jsonString = new String(payload, StandardCharsets.UTF_8);
      JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();

      if (schema != null) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonString);
        List<Error> validationResult = schema.validate(jsonNode);
        if (!validationResult.isEmpty()) {
          logger.log(JSON_PARSE_EXCEPTION, getName(), validationResult);
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
  public byte[] parseFromJson(JsonObject jsonObject) throws IOException {
    return gson.toJson(jsonObject).getBytes(StandardCharsets.UTF_8);
  }


  @Override
  public MessageFormatter getInstance(SchemaConfig config) {
    // Extract the JSON Schema string from the version’s schema field (JsonElement recommended)
    String schemaString = null;
    if (config.getSchema() != null) {
      JsonElement el = config.getSchema();
      schemaString = el.isJsonPrimitive() ? el.getAsString() : el.toString();
    }
    try {
      return new JsonFormatter(schemaString);
    } catch (JsonProcessingException e) {

    }
    return null;
  }

  @Override
  public String getName() {
    return "JSON";
  }

}
