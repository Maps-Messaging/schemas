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

package io.mapsmessaging.schemas.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.schemas.config.impl.CborSchemaConfig;
import org.junit.jupiter.api.Assertions;

public class TestCborConfig extends GeneralBaseTest {

  SchemaConfig getProperties() {
    CborSchemaConfig props = new CborSchemaConfig();
    JsonObject obj = JsonParser.parseString(cborSchema).getAsJsonObject();
    props.setSchema(obj);
    return props;
  }

  @Override
  SchemaConfig buildConfig() {
    CborSchemaConfig config = new CborSchemaConfig();
    setBaseConfig(config);
    JsonObject obj = JsonParser.parseString(cborSchema).getAsJsonObject();
    config.setSchema(obj);
    return config;
  }

  @Override
  void validate(SchemaConfig schemaConfig) {
    Assertions.assertInstanceOf(CborSchemaConfig.class, schemaConfig);
  }

  private static final String cborSchema = "{\n" +
      "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
      "  \"type\": \"object\",\n" +
      "  \"properties\": {\n" +
      "    \"stringId\": { \"type\": \"string\" },\n" +
      "    \"longId\": { \"type\": \"number\" },\n" +
      "    \"intId\": { \"type\": \"number\" },\n" +
      "    \"floatId\": { \"type\": \"number\" },\n" +
      "    \"doubleId\": { \"type\": \"number\" }\n" +
      "  },\n" +
      " \"required\": [\"stringId\", \"longId\", \"intId\", \"floatId\", \"doubleId\"],\n" +
      "  \"additionalProperties\": false\n" +
      "}";
}