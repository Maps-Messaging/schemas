/*
 *
 *     Copyright [ 2020 - 2026 ] [Matthew Buckton]
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

package io.mapsmessaging.schemas.config;

import io.mapsmessaging.schemas.config.impl.MavlinkSchemaConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.UUID;

public class TestMavlinkConfig extends GeneralBaseTest {

  private static final String DIALECT = "common";

  private static void assertCommonXmlPresent() throws IOException {
    try (InputStream stream = TestMavlinkConfig.class.getClassLoader().getResourceAsStream("mavlink/common.xml")) {
      Assertions.assertNotNull(stream, "Missing resource mavlink/common.xml");
      byte[] tmp = new byte[16];
      int read = stream.read(tmp);
      Assertions.assertTrue(read > 0, "mavlink/common.xml is empty/unreadable");
    }
  }

  @Override
  SchemaConfig getProperties() throws IOException {
    MavlinkSchemaConfig config = new MavlinkSchemaConfig();
    config.setDialect(DIALECT);
    return config;
  }

  @Override
  SchemaConfig buildConfig() throws IOException {
    assertCommonXmlPresent();

    MavlinkSchemaConfig config = new MavlinkSchemaConfig();
    config.setDialect(DIALECT);
    setBaseConfig(config);
    return config;
  }

  @Override
  void validate(SchemaConfig schemaConfig) {
    Assertions.assertInstanceOf(MavlinkSchemaConfig.class, schemaConfig);
    MavlinkSchemaConfig mavlink = (MavlinkSchemaConfig) schemaConfig;

    Assertions.assertEquals("mavlink", mavlink.getFormat());
    Assertions.assertEquals(DIALECT, mavlink.getDialect());
    Assertions.assertNotNull(mavlink.getSchema());
    Assertions.assertTrue(mavlink.getSchema().has("dialect"));
    Assertions.assertEquals(DIALECT, mavlink.getSchema().get("dialect").getAsString());
  }

  @Test
  void invalidConfig() {
    MavlinkSchemaConfig config = new MavlinkSchemaConfig();
    config.setUniqueId(UUID.randomUUID());
    config.setExpiresAfter(OffsetDateTime.now().plusDays(10));
    config.setNotBefore(OffsetDateTime.now().minusDays(10));

    Assertions.assertThrowsExactly(IOException.class, config::pack);
  }
}
