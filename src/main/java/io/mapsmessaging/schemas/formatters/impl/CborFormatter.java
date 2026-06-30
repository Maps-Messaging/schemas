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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.networknt.schema.*;
import com.networknt.schema.Error;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParseException;
import io.mapsmessaging.schemas.formatters.ParseMode;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.walker.MapResolver;
import io.mapsmessaging.schemas.formatters.walker.StructuredResolver;
import io.mapsmessaging.schemas.repository.SchemaResolver;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.FORMATTER_UNEXPECTED_OBJECT;
import static io.mapsmessaging.schemas.logging.SchemaLogMessages.JSON_PARSE_EXCEPTION;

public class CborFormatter extends MessageFormatter {

  private final ObjectMapper cborMapper;
  private final ObjectMapper jsonMapper;
  private final Gson gson;
  private final JsonNode schemaNode;
  private final Schema schema;

  public CborFormatter() {
    cborMapper = new ObjectMapper(new CBORFactory());
    jsonMapper = new ObjectMapper();
    gson = new Gson();
    schemaNode = null;
    schema = null;
  }

  public CborFormatter(String schemaString) throws IOException {
    cborMapper = new ObjectMapper(new CBORFactory());
    jsonMapper = new ObjectMapper();
    gson = new Gson();

    if (schemaString == null || schemaString.isBlank() || "null".equals(schemaString.trim())) {
      schemaNode = null;
      schema = null;
      return;
    }

    schemaNode = jsonMapper.readTree(schemaString);
    if (schemaNode == null || schemaNode.isNull() || !schemaNode.isObject()) {
      schema = null;
      return;
    }

    String schemaDialect = schemaNode.path("$schema").asText(null);
    SpecificationVersion version = SpecificationVersion.fromDialectId(schemaDialect)
        .orElse(SpecificationVersion.DRAFT_7);

    SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(version);
    schema = schemaRegistry.getSchema(schemaNode.toString(), InputFormat.JSON);
  }

  @Override
  public ParsedObject parse(byte[] payload, ParseMode parseMode) throws ParseException {
    try {
      Map<String, Object> map = cborMapper.readValue(payload, Map.class);

      if (schema != null) {
        String jsonString = jsonMapper.writeValueAsString(map);
        List<Error> validationResult = schema.validate(jsonString, InputFormat.JSON);

        if (!validationResult.isEmpty()) {
          logger.log(JSON_PARSE_EXCEPTION, getName(), validationResult);
          if (parseMode == ParseMode.STRICT) {
            throw new ParseException(validationResult.toString());
          }
          return new DefaultParser(payload);
        }
      }

      JsonObject json = gson.toJsonTree(map).getAsJsonObject();
      return new StructuredResolver(new MapResolver(map), json);
    } catch (Exception e) {
      logger.log(FORMATTER_UNEXPECTED_OBJECT, getName(), payload);
      if (parseMode == ParseMode.IGNORE) {
        return new DefaultParser(payload);
      }
      throw new ParseException(e.getMessage(), e);
    }
  }

  @Override
  public JsonObject parseToJson(byte[] payload, ParseMode parseMode) throws ParseException {
    try {
      Map<String, Object> map = cborMapper.readValue(payload, Map.class);
      return JsonParser.parseString(gson.toJson(map)).getAsJsonObject();
    } catch (IOException e) {
      throw new ParseException(e.getMessage(), e);
    }
  }

  @Override
  public byte[] parseFromJson(JsonObject jsonObject) throws IOException {
    @SuppressWarnings("unchecked")
    Map<String, Object> map = gson.fromJson(jsonObject, Map.class);

    return cborMapper.writeValueAsBytes(map);
  }

  @Override
  public MessageFormatter getInstance(SchemaConfig config, SchemaResolver schemaResolver) throws IOException {
    JsonObject configSchema = config.getSchema();
    if (configSchema == null || configSchema.isJsonNull() || configSchema.isEmpty()) {
      return new CborFormatter();
    }
    return new CborFormatter(SchemaConfig.gson.toJson(configSchema));
  }

  @Override
  public String getName() {
    return "CBOR";
  }

  @Override
  public Map<String, Object> getFormat() {
    if (schemaNode == null || !schemaNode.has("properties")) {
      return Map.of();
    }

    try {
      JsonNode propertiesNode = schemaNode.get("properties");
      Map<String, Object> result = new LinkedHashMap<>();

      propertiesNode.fields().forEachRemaining(entry -> {
        String fieldName = entry.getKey();
        JsonNode attributes = entry.getValue();
        Map<String, Object> attrMap = jsonMapper.convertValue(attributes, Map.class);
        result.put(fieldName, attrMap);
      });

      return result;
    } catch (Exception e) {
      logger.log(FORMATTER_UNEXPECTED_OBJECT, getName(), e.getMessage());
      return Map.of();
    }
  }
}