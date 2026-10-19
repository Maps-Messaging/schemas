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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MavlinkSchemaConfigCoverageTest {

  @Test
  void dialectAndXmlFormsAreMutuallyExclusiveBySetters() {
    MavlinkSchemaConfig config = new MavlinkSchemaConfig();

    config.setDialect("COMMON");
    assertEquals("common", config.getDialect());

    config.setDialectXml("<mavlink/>");
    assertEquals("<mavlink/>", config.getDialectXml());

    config.setDialectXmlBase64("<base64/>".getBytes(StandardCharsets.UTF_8));
    assertEquals("<base64/>", config.getDialectXml());

    config.setDialectXmlBase64(new byte[0]);
    assertNull(config.getDialectXml());

    config.setDialect(" ");
    assertNull(config.getDialect());
  }

  @Test
  void packRequiresExactlyOneSource() throws Exception {
    MavlinkSchemaConfig config = new MavlinkSchemaConfig();
    assertThrows(IOException.class, config::pack);

    config.setDialect("common");
    assertTrue(config.pack().contains("common"));

    config.setDialect(null);
    config.setDialectXml("<mavlink/>");
    assertTrue(config.pack().contains("dialectXml"));
  }
}
