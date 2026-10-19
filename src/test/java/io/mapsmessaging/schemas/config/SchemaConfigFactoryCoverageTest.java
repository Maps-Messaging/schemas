/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 *
 */

package io.mapsmessaging.schemas.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonObject;
import io.mapsmessaging.schemas.config.impl.JsonSchemaConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SchemaConfigFactoryCoverageTest {

  private final SchemaConfigFactory factory = SchemaConfigFactory.getInstance();

  @Test
  void detectFormatWalksExpectedLegacyShape() {
    Map<String, Object> schema = Map.of("format", "json");
    Map<String, Object> schemas = Map.of("id", schema);
    Map<String, Object> group = Map.of("schemas", schemas);
    Map<String, Object> groups = Map.of("id", group);
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("uuid", "id");
    root.put("schemagroups", groups);

    assertEquals("json", SchemaConfigFactory.detectFormat(root));

    root.put("schemagroups", "wrong");
    assertNull(SchemaConfigFactory.detectFormat(root));
  }

  @Test
  void detectFormatReturnsNullForMissingNestedMapsAndFormat() {
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("uuid", "id");
    root.put("schemagroups", Map.of());
    assertNull(SchemaConfigFactory.detectFormat(root));

    root.put("schemagroups", Map.of("id", Map.of("schemas", "wrong")));
    assertNull(SchemaConfigFactory.detectFormat(root));

    root.put("schemagroups", Map.of("id", Map.of("schemas", Map.of())));
    assertNull(SchemaConfigFactory.detectFormat(root));

    root.put(
        "schemagroups",
        Map.of("id", Map.of("schemas", Map.of("id", Map.of())))
    );
    assertNull(SchemaConfigFactory.detectFormat(root));
  }

  @Test
  void constructConfigSupportsBytesStringsObjectsAndMaps() throws Exception {
    String json = """
        {
          "format": "json",
          "schema": { "type": "object" }
        }
        """;

    assertInstanceOf(JsonSchemaConfig.class, factory.constructConfig(json));
    assertInstanceOf(
        JsonSchemaConfig.class,
        factory.constructConfig(json.getBytes(StandardCharsets.UTF_8))
    );

    JsonObject object = SchemaConfigFactory.gson.fromJson(json, JsonObject.class);
    assertInstanceOf(JsonSchemaConfig.class, factory.constructConfig(object));

    @SuppressWarnings("deprecation")
    SchemaConfig fromMap = factory.constructConfig(
        Map.of("format", "json", "schema", Map.of("type", "object"))
    );
    assertInstanceOf(JsonSchemaConfig.class, fromMap);
  }

  @Test
  void constructConfigRejectsEmptyMalformedAndUnknownInputs() {
    assertThrows(IllegalStateException.class, () -> factory.constructConfig((byte[]) null));
    assertThrows(IllegalStateException.class, () -> factory.constructConfig(new byte[0]));
    assertThrows(IllegalStateException.class, () -> factory.constructConfig((Map<String, Object>) null));
    assertThrows(IllegalStateException.class, () -> factory.constructConfig(Map.of()));

    assertThrows(IOException.class, () -> factory.constructConfig("{"));
    assertThrows(IOException.class, () -> factory.constructConfig("{}"));

    SchemaConfig unknown = new SchemaConfig();
    unknown.setFormat("unsupported");
    assertNull(factory.constructConfig(unknown));
  }
}
