
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


import io.mapsmessaging.schemas.config.impl.ProtoBufSchemaConfig;
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

class TestProtobufConfig extends GeneralBaseTest {

  Map<String, Object> getProperties() throws IOException {
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("format", "ProtoBuf");
    props.put("descriptor", new String(Base64.getEncoder().encode(getDescriptor())));
    props.put("messageName", "Person");
    return props;
  }

  @Override
  SchemaConfig buildConfig() throws IOException {
    ProtoBufSchemaConfig config = new ProtoBufSchemaConfig();
    config.setDescriptorValue(getDescriptor());
    config.setMessageName("Person");
    config.setUniqueId(UUID.randomUUID());
    config.setExpiresAfter(LocalDateTime.now().plusDays(10));
    config.setNotBefore(LocalDateTime.now().minusDays(10));
    return config;
  }

  @Override
  void validate(SchemaConfig schemaConfig) throws IOException {
    Assertions.assertTrue(schemaConfig instanceof ProtoBufSchemaConfig);
    ProtoBufSchemaConfig config = (ProtoBufSchemaConfig) schemaConfig;
    Assertions.assertArrayEquals(getDescriptor(), config.getDescriptorValue());
    Assertions.assertEquals("Person", config.getMessageName());
  }


  private byte[] getDescriptor() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(10240);
    byte[] tmp = new byte[10240];
    try (InputStream fis = TestProtobufConfig.class.getClassLoader().getResourceAsStream("Person.desc")) {
      int len = fis.read(tmp);
      baos.write(tmp, 0, len);
    }
    return baos.toByteArray();
  }

  @Test
  void invalidConfigWithName() {
    ProtoBufSchemaConfig config = new ProtoBufSchemaConfig();
    config.setMessageName("justAName");
    config.setUniqueId(UUID.randomUUID());
    config.setExpiresAfter(LocalDateTime.now().plusDays(10));
    config.setNotBefore(LocalDateTime.now().minusDays(10));
    Assertions.assertThrowsExactly(IOException.class, () -> config.pack());
  }

  @Test
  void invalidConfigWithDescriptor() throws IOException {
    ProtoBufSchemaConfig config = new ProtoBufSchemaConfig();
    config.setDescriptorValue(getDescriptor());
    config.setUniqueId(UUID.randomUUID());
    config.setExpiresAfter(LocalDateTime.now().plusDays(10));
    config.setNotBefore(LocalDateTime.now().minusDays(10));
    Assertions.assertThrowsExactly(IOException.class, () -> config.pack());
  }


  @Test
  void invalidConfig() {
    ProtoBufSchemaConfig config = new ProtoBufSchemaConfig();
    config.setUniqueId(UUID.randomUUID());
    config.setExpiresAfter(LocalDateTime.now().plusDays(10));
    config.setNotBefore(LocalDateTime.now().minusDays(10));
    Assertions.assertThrowsExactly(IOException.class, () -> config.pack());
  }
}
