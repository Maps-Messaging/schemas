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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.schemas.config.SchemaConfig;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class CanAerospaceSchemaConfigTest {

  @Test
  void exposesFormatMimeTypeAndCopiesConfig() {
    CanAerospaceSchemaConfig config = new CanAerospaceSchemaConfig();

    assertEquals("canaerospace", config.getFormat());
    assertEquals("application/octet-stream", config.getMimeType());

    SchemaConfig source = new SchemaConfig();
    source.setName("copy");
    source.setFormat("canaerospace");

    SchemaConfig copy = config.getInstance(source);
    assertInstanceOf(CanAerospaceSchemaConfig.class, copy);
    assertEquals("copy", copy.getName());
  }

  @Test
  void yamlPathCanBeSetReadAndRemoved() {
    CanAerospaceSchemaConfig config = new CanAerospaceSchemaConfig();

    assertNull(config.getYamlPath());

    config.setYamlPath("/tmp/schema.yaml");
    assertEquals("/tmp/schema.yaml", config.getYamlPath());

    config.setYamlPath(" ");
    assertNull(config.getYamlPath());

    config.setYamlPath(null);
    assertNull(config.getYamlPath());
  }

  @Test
  void packRequiresYamlPathAndSerialisesWhenPresent() throws Exception {
    CanAerospaceSchemaConfig config = new CanAerospaceSchemaConfig();

    IOException missing = assertThrows(IOException.class, config::pack);
    assertTrue(missing.getMessage().contains("dialect"));

    config.setYamlPath("/tmp/schema.yaml");
    String packed = config.pack();

    assertTrue(packed.contains("YamlPath"));
    assertTrue(packed.contains("/tmp/schema.yaml"));
  }
}
