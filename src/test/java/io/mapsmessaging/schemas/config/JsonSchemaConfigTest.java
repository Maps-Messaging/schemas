/*
 *
 *  Copyright [ 2020 - 2026 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import io.mapsmessaging.schemas.config.impl.JsonSchemaConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JsonSchemaConfigTest {

  @TempDir
  Path tempDirectory;

  @Test
  void pathConstructorLoadsSchemaAndPreservesRef() throws Exception {
    Path schemaFile = tempDirectory.resolve("schema.json");

    Files.writeString(schemaFile, """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "$ref": "#/$defs/Person",
          "$defs": {
            "Person": {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                }
              },
              "required": ["name"]
            }
          }
        }
        """);

    JsonSchemaConfig config = new JsonSchemaConfig(schemaFile);

    JsonObject schema = config.getSchema();
    assertNotNull(schema);
    assertEquals("schema.json", config.getName());
    assertEquals("application/json", config.getMimeType());

    assertTrue(schema.has("$ref"));
    assertEquals("#/$defs/Person", schema.get("$ref").getAsString());

    assertTrue(schema.has("$defs"));
    JsonObject defs = schema.getAsJsonObject("$defs");
    assertTrue(defs.has("Person"));

    JsonObject person = defs.getAsJsonObject("Person");
    assertEquals("object", person.get("type").getAsString());
    assertTrue(person.has("properties"));
    assertTrue(person.has("required"));
  }
}