/*
 *
 *     Copyright [ 2020 - 2022 ] [Matthew Buckton]
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

import io.mapsmessaging.schemas.config.impl.AvroSchemaConfig;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestAvroConfig extends GeneralBaseTest {

  public static String getSchema() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(10240);
    byte[] tmp = new byte[10240];
    try (InputStream fis = TestProtobufConfig.class.getClassLoader().getResourceAsStream("avro/Person.avsc")) {
      int len = fis.read(tmp);
      baos.write(tmp, 0, len);
    }
    return baos.toString();
  }

  Map<String, Object> getProperties() throws IOException {
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("format", "AVRO");
    props.put("schema", new String(Base64.getEncoder().encode(getSchema().getBytes())));
    return props;
  }

  @Override
  SchemaConfig buildConfig() throws IOException {
    AvroSchemaConfig config = new AvroSchemaConfig();
    config.setSchema(getSchema());
    config.setUniqueId(UUID.randomUUID());
    config.setExpiresAfter(LocalDateTime.now().plusDays(10));
    config.setNotBefore(LocalDateTime.now().minusDays(10));
    config.setComments("Unit Tests");
    config.setSource("tcp://localhost:1883/topic2");
    return config;
  }

  @Override
  void validate(SchemaConfig schemaConfig) throws IOException {
    Assertions.assertTrue(schemaConfig instanceof AvroSchemaConfig);
    AvroSchemaConfig config = (AvroSchemaConfig) schemaConfig;
    Assertions.assertEquals(getSchema(), config.getSchema());
  }

  @Test
  void invalidConfig() {
    AvroSchemaConfig config = new AvroSchemaConfig();
    config.setUniqueId(UUID.randomUUID());
    config.setExpiresAfter(LocalDateTime.now().plusDays(10));
    config.setNotBefore(LocalDateTime.now().minusDays(10));
    Assertions.assertThrowsExactly(IOException.class, () -> config.pack());
  }
}
