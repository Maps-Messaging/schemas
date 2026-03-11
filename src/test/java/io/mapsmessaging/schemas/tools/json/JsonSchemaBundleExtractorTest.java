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

package io.mapsmessaging.schemas.tools.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonSchemaPointerResolverTest {

  @Test
  void testResolveDefinitionPointer() {
    JsonObject schema = JsonParser.parseString("""
        {
          "$schema": "https://json-schema.org/draft/2019-09/schema#",
          "$defs": {
            "Track": {
              "type": "object",
              "properties": {
                "id": {
                  "type": "string"
                }
              }
            }
          }
        }
        """).getAsJsonObject();

    JsonElement resolved = JsonSchemaPointerResolver.resolve(schema, "#/$defs/Track");

    assertTrue(resolved.isJsonObject());
    assertEquals("object", resolved.getAsJsonObject().get("type").getAsString());
    assertTrue(resolved.getAsJsonObject().has("properties"));
  }

  @Test
  void testResolveEscapedPointerToken() {
    JsonObject schema = JsonParser.parseString("""
        {
          "$defs": {
            "A/B~C": {
              "type": "object"
            }
          }
        }
        """).getAsJsonObject();

    JsonElement resolved = JsonSchemaPointerResolver.resolve(schema, "#/$defs/A~1B~0C");

    assertTrue(resolved.isJsonObject());
    assertEquals("object", resolved.getAsJsonObject().get("type").getAsString());
  }

  @Test
  void testResolveRootPointer() {
    JsonObject schema = JsonParser.parseString("""
        {
          "$defs": {
            "Track": {
              "type": "object"
            }
          }
        }
        """).getAsJsonObject();

    JsonElement resolved = JsonSchemaPointerResolver.resolve(schema, "#");

    assertSame(schema, resolved);
  }

  @Test
  void testThrowsForMissingToken() {
    JsonObject schema = JsonParser.parseString("""
        {
          "$defs": {
            "Track": {
              "type": "object"
            }
          }
        }
        """).getAsJsonObject();

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> JsonSchemaPointerResolver.resolve(schema, "#/$defs/Unknown")
    );

    assertTrue(exception.getMessage().contains("Pointer token not found"));
  }
}