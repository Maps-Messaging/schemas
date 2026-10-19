/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 *
 */

package io.mapsmessaging.schemas.config.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.schemas.config.SchemaConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CanbusSchemaConfigCoverageTest {

  @Test
  void exposesFormatMimeTypeAndCopiesConfig() {
    CanbusSchemaConfig config = new CanbusSchemaConfig();

    assertEquals("canbus", config.getFormat());
    assertEquals("application/octet-stream", config.getMimeType());

    SchemaConfig source = new SchemaConfig();
    source.setName("copy");
    source.setFormat("canbus");

    SchemaConfig copy = config.getInstance(source);
    assertTrue(copy instanceof CanbusSchemaConfig);
    assertEquals("copy", copy.getName());
  }

  @Test
  void pathAndBase64AreMutuallyExclusive() {
    CanbusSchemaConfig config = new CanbusSchemaConfig();

    config.setXmlPath("/tmp/schema.xml");
    assertEquals("/tmp/schema.xml", config.getXmlPath());
    assertNull(config.getXmlBase64());

    byte[] xml = "<root/>".getBytes(StandardCharsets.UTF_8);
    config.setXmlBase64(xml);
    assertArrayEquals(xml, config.getXmlBase64());
    assertNull(config.getXmlPath());

    config.setXmlBase64(new byte[0]);
    assertNull(config.getXmlBase64());
    assertNull(config.getXmlPath());
  }

  @Test
  void blankPathRemovesStoredPath() {
    CanbusSchemaConfig config = new CanbusSchemaConfig();

    config.setXmlPath("/tmp/schema.xml");
    config.setXmlPath(" ");

    assertNull(config.getXmlPath());
  }

  @Test
  void packRequiresExactlyOneSchemaSource() throws Exception {
    CanbusSchemaConfig empty = new CanbusSchemaConfig();
    assertThrows(IOException.class, empty::pack);

    CanbusSchemaConfig path = new CanbusSchemaConfig();
    path.setXmlPath("/tmp/schema.xml");
    assertTrue(path.pack().contains("XmlPath"));

    CanbusSchemaConfig inline = new CanbusSchemaConfig();
    inline.setXmlBase64("<root/>".getBytes(StandardCharsets.UTF_8));
    assertTrue(inline.pack().contains("XmlBase64"));
  }
}
