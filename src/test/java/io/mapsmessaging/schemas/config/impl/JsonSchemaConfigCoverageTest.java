/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 *
 */

package io.mapsmessaging.schemas.config.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.schemas.config.SchemaConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonSchemaConfigCoverageTest {

  @TempDir
  Path tempDirectory;

  @Test
  void constructorsExposeExpectedDefaultsAndMimeType() throws Exception {
    JsonSchemaConfig empty = new JsonSchemaConfig();
    assertEquals("json", empty.getFormat());
    assertEquals("application/json", empty.getMimeType());
    assertFalse(empty.isBundle());

    JsonSchemaConfig fromString = new JsonSchemaConfig("{\"type\":\"string\"}");
    assertEquals("string", fromString.getSchema().get("type").getAsString());

    Path file = tempDirectory.resolve("schema.json");
    Files.writeString(file, "{\"type\":\"number\"}");
    JsonSchemaConfig fromFile = new JsonSchemaConfig(file);

    assertEquals("schema.json", fromFile.getName());
    assertEquals("1", fromFile.getVersion());
    assertTrue(fromFile.getSource().startsWith("file:"));
    assertEquals("number", fromFile.getSchema().get("type").getAsString());
  }

  @Test
  void bundleExpansionCreatesChildSchemasAndEscapesPointers() {
    JsonSchemaConfig config = new JsonSchemaConfig("""
        {
          "$defs": {
            "a/b": { "type": "string" },
            "til~de": { "type": "integer" }
          }
        }
        """);
    config.setUniqueId("root");
    config.setSource("source");
    config.setVersion("7");

    assertTrue(config.isBundle());

    List<SchemaConfig> children = config.getBundledSchemas();
    assertEquals(2, children.size());
    assertEquals(children, config.getBundledSchemas());

    SchemaConfig slash = children.stream()
        .filter(child -> child.getName().endsWith("a/b"))
        .findFirst()
        .orElseThrow();

    assertEquals("root", slash.getParentUuid());
    assertEquals("7", slash.getVersion());
    assertEquals("#/$defs/a~1b", slash.getSource());
    assertEquals("string", slash.getSchema().get("type").getAsString());

    SchemaConfig tilde = children.stream()
        .filter(child -> child.getName().endsWith("til~de"))
        .findFirst()
        .orElseThrow();
    assertEquals("#/$defs/til~0de", tilde.getSource());
  }

  @Test
  void nonObjectOrEmptyDefsAreNotBundles() {
    assertFalse(new JsonSchemaConfig("{}").isBundle());
    assertFalse(new JsonSchemaConfig("{\"$defs\":{}}").isBundle());
    assertFalse(new JsonSchemaConfig("{\"$defs\":[]}}").isBundle());
  }

  @Test
  void getInstancePreservesBaseConfig() {
    SchemaConfig base = new SchemaConfig();
    base.setName("copied");
    base.setFormat("json");

    SchemaConfig copy = new JsonSchemaConfig().getInstance(base);

    assertInstanceOf(JsonSchemaConfig.class, copy);
    assertEquals("copied", copy.getName());
  }

  @Test
  void invalidJsonStringIsRejected() {
    assertThrows(RuntimeException.class, () -> new JsonSchemaConfig("not-json"));
  }
}
