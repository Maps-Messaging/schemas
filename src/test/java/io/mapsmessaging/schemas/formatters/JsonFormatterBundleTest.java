/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.schemas.formatters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import io.mapsmessaging.schemas.formatters.impl.JsonFormatter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonFormatterBundleTest {

  private static final String ROOT_SCHEMA = """
      {
        "$schema": "https://json-schema.org/draft/2019-09/schema#",
        "$id": "file:///catl_hibw.json",
        "$defs": {
          "CommonHeader": {
            "type": "object",
            "properties": {
              "source": {
                "type": "string"
              }
            },
            "required": [
              "source"
            ]
          },
          "Track": {
            "type": "object",
            "properties": {
              "id": {
                "type": "string"
              },
              "header": {
                "$ref": "#/$defs/CommonHeader"
              }
            },
            "required": [
              "id",
              "header"
            ]
          }
        }
      }
      """;

  @Test
  void testChildFormatterExposesSelectedDefinitionProperties() throws Exception {
    JsonFormatter formatter = new JsonFormatter(ROOT_SCHEMA, "#/$defs/Track");

    Map<String, Object> format = formatter.getFormat();

    assertEquals(2, format.size());
    assertTrue(format.containsKey("id"));
    assertTrue(format.containsKey("header"));
  }

  @Test
  void testChildFormatterValidatesPayloadUsingParentContext() throws Exception {
    JsonFormatter formatter = new JsonFormatter(ROOT_SCHEMA, "#/$defs/Track");
    Schema schema = extractSchema(formatter);

    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode validPayload = objectMapper.readTree("""
        {
          "id": "TRACK-001",
          "header": {
            "source": "sensor-A"
          }
        }
        """);

    List<Error> errors = schema.validate(validPayload);

    assertTrue(errors.isEmpty(), "Expected no validation errors but got: " + errors);
  }

  @Test
  void testChildFormatterFailsWhenReferencedDefinitionValidationFails() throws Exception {
    JsonFormatter formatter = new JsonFormatter(ROOT_SCHEMA, "#/$defs/Track");
    Schema schema = extractSchema(formatter);

    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode invalidPayload = objectMapper.readTree("""
        {
          "id": "TRACK-001",
          "header": {
            "source": 100
          }
        }
        """);

    List<Error> errors = schema.validate(invalidPayload);

    assertFalse(errors.isEmpty());
  }

  @Test
  void testChildFormatterFailsWhenRequiredReferencedObjectMissing() throws Exception {
    JsonFormatter formatter = new JsonFormatter(ROOT_SCHEMA, "#/$defs/Track");
    Schema schema = extractSchema(formatter);

    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode invalidPayload = objectMapper.readTree("""
        {
          "id": "TRACK-001"
        }
        """);

    List<Error> errors = schema.validate(invalidPayload);

    assertFalse(errors.isEmpty());
  }

  private Schema extractSchema(JsonFormatter formatter) throws Exception {
    Field field = JsonFormatter.class.getDeclaredField("schema");
    field.setAccessible(true);
    return (Schema) field.get(formatter);
  }
}