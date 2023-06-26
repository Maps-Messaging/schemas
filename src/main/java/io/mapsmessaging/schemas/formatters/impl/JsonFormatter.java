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

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.JsonSchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.walker.MapResolver;
import io.mapsmessaging.schemas.formatters.walker.StructuredResolver;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.FORMATTER_UNEXPECTED_OBJECT;

/**
 * The type Json formatter.
 */
public class JsonFormatter extends MessageFormatter {

  private final Schema schema;

  /**
   * Instantiates a new Json formatter.
   */
  public JsonFormatter() {
    schema = null;
  }


  public JsonFormatter(String schemaString) {
    if(schemaString != null && schemaString.length() > 0) {
      schema = SchemaLoader.load(new JSONObject(schemaString));
    }
    else{
      schema = null;
    }
  }

  @Override
  public ParsedObject parse(byte[] payload) {
    try {
      JSONObject json = new JSONObject(new String(payload));
      if (schema != null) {
        schema.validate(json);
      }
      return new StructuredResolver(new MapResolver(json.toMap()), json);
    } catch (JSONException e) {
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
