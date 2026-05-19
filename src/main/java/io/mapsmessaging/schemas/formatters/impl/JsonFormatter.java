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
import io.mapsmessaging.schemas.tools.json.JsonSchemaPointerResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.FORMATTER_UNEXPECTED_OBJECT;
import static io.mapsmessaging.schemas.logging.SchemaLogMessages.JSON_PARSE_EXCEPTION;

/**
 * The type Json formatter.
 */
public class JsonFormatter extends MessageFormatter {

  private final JsonNode rootSchemaNode;
  private final JsonNode selectedSchemaNode;
  private final Schema schema;
  private final String definitionPointer;

  /**
   * Instantiates a new Json formatter.
   */
  public JsonFormatter() {
    rootSchemaNode = null;
    selectedSchemaNode = null;
    schema = null;
    definitionPointer = null;
  }

  public JsonFormatter(String schemaString) throws JsonProcessingException {
    this(schemaString, null);
  }

  public JsonFormatter(String schemaString, String definitionPointer) throws JsonProcessingException {
    if (schemaString == null || schemaString.isEmpty()) {
      throw new JsonProcessingException("Schema string is null or empty") {
      };
    }

    ObjectMapper objectMapper = new ObjectMapper();
    rootSchemaNode = objectMapper.readTree(schemaString);
    this.definitionPointer = definitionPointer;

    JsonNode effectiveSchemaNode;
    if (definitionPointer != null && !definitionPointer.isEmpty()) {
      effectiveSchemaNode = buildChildWrapperSchema(rootSchemaNode, definitionPointer);
      JsonObject rootObject = JsonParser.parseString(schemaString).getAsJsonObject();
      JsonElement selected = JsonSchemaPointerResolver.resolve(rootObject, definitionPointer);
      selectedSchemaNode = objectMapper.readTree(selected.toString());
    } else {
      effectiveSchemaNode = rootSchemaNode;
      selectedSchemaNode = rootSchemaNode;
    }

    String schemaDialect = rootSchemaNode.path("$schema").asText(null);
    SpecificationVersion version = SpecificationVersion.fromDialectId(schemaDialect).orElse(SpecificationVersion.DRAFT_7);
    SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(version);
    schema = schemaRegistry.getSchema(effectiveSchemaNode);
  }

  public JsonFormatter(Path schemaPath) throws IOException {
    if (schemaPath == null) {
      throw new IllegalArgumentException("schemaPath cannot be null");
    }
    if (!Files.exists(schemaPath)) {
      throw new IOException("Schema file does not exist: " + schemaPath);
    }
    if (!Files.isRegularFile(schemaPath)) {
      throw new IOException("Schema path is not a file: " + schemaPath);
    }

    ObjectMapper objectMapper = new ObjectMapper();
    rootSchemaNode = objectMapper.readTree(schemaPath.toFile());
    selectedSchemaNode = rootSchemaNode;
    definitionPointer = null;

    String schemaDialect = rootSchemaNode.path("$schema").asText(null);
    SpecificationVersion version = SpecificationVersion.fromDialectId(schemaDialect)
        .orElse(SpecificationVersion.DRAFT_7);

    SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(version);
    SchemaLocation location = SchemaLocation.of(schemaPath.toUri().toString());
    schema = schemaRegistry.getSchema(location);
    schema.initializeValidators();
  }

  @Override
  public ParsedObject parse(byte[] payload, ParseMode parseMode) throws ParseException {
    try {
      String jsonString = new String(payload, StandardCharsets.UTF_8);
      JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();

      if (schema != null) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonString);
        List<Error> validationResult = schema.validate(jsonNode);
        if (!validationResult.isEmpty()) {
          logger.log(JSON_PARSE_EXCEPTION, getName(), validationResult);
          if (parseMode == ParseMode.STRICT) {
            throw new ParseException(validationResult.toString());
          }
        }
      }

      Gson gson = new Gson();
      @SuppressWarnings("unchecked")
      Map<String, Object> map = gson.fromJson(json, Map.class);
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
  public Map<String, Object> getFormat() {
    if (selectedSchemaNode == null) {
      return Map.of();
    }
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode node = selectedSchemaNode.get("properties");
      if (node == null) {
        node = selectedSchemaNode.get("_children");
      }
      if (node == null) {
        node = selectedSchemaNode;
      }

      return objectMapper.convertValue(node, Map.class);
    } catch (Exception e) {
      logger.log(FORMATTER_UNEXPECTED_OBJECT, getName(), e.getMessage());
      return Map.of();
    }
  }

  @Override
  public JsonObject parseToJson(byte[] payload, ParseMode parseMode) throws ParseException {
    return JsonParser.parseString(new String(payload, StandardCharsets.UTF_8)).getAsJsonObject();
  }

  @Override
  public byte[] parseFromJson(JsonObject jsonObject) throws IOException {
    return gson.toJson(jsonObject).getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public MessageFormatter getInstance(SchemaConfig config, SchemaResolver schemaResolver) {
    String schemaString = null;
    String pointer = null;

    try {
      if (config.isChild()) {
        SchemaConfig parent = schemaResolver.resolveParent(config);
        if (parent != null && parent.getSchema() != null) {
          JsonElement parentElement = parent.getSchema();
          schemaString = parentElement.isJsonPrimitive() ? parentElement.getAsString() : parentElement.toString();
          pointer = config.getSource();
        }
      } else {
        if (config.getSchema() != null) {
          JsonElement element = config.getSchema();
          schemaString = element.isJsonPrimitive() ? element.getAsString() : element.toString();
        }
      }

      if (schemaString == null || schemaString.isEmpty()) {
        return null;
      }

      return new JsonFormatter(schemaString, pointer);
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public String getName() {
    return "JSON";
  }

  private JsonNode buildChildWrapperSchema(JsonNode rootSchema, String pointer) {
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode defsNode = rootSchema.get("$defs");

    com.fasterxml.jackson.databind.node.ObjectNode wrapper = objectMapper.createObjectNode();

    if (rootSchema.has("$schema")) {
      wrapper.set("$schema", rootSchema.get("$schema"));
    }

    if (rootSchema.has("$id")) {
      wrapper.set("$id", rootSchema.get("$id"));
    }

    if (defsNode != null) {
      wrapper.set("$defs", defsNode);
    }

    wrapper.put("$ref", pointer);
    return wrapper;
  }
}