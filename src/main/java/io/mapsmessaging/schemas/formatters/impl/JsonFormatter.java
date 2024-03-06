/*
 *
 *     Copyright [ 2020 - 2023 ] [Matthew Buckton]
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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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
      JSONObject json = new JSONObject(new String(payload));

      if (schema != null) {
        ObjectMapper objectMapper = new ObjectMapper();

        // Parse JSON string to JsonNode
        JsonNode jsonNode = objectMapper.readTree(payload);
        Set<ValidationMessage> validationResult = schema.validate(jsonNode);
        if (!validationResult.isEmpty()) {
          logger.log(JSON_PARSE_EXCEPTION, getName(), validationResult);
          return new DefaultParser(payload);
        }
      }
      return new StructuredResolver(new MapResolver(json.toMap()), json);
    } catch (JSONException | IOException e) {
      logger.log(FORMATTER_UNEXPECTED_OBJECT, getName(), payload);
      return new DefaultParser(payload);
    }
  }

  @Override
  public JSONObject parseToJson(byte[] payload) throws IOException {
    return new JSONObject(new String(payload));
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
