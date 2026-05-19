/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.schemas.config;


import io.mapsmessaging.schemas.config.impl.ProtoBufSchemaConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.UUID;

class TestProtobufConfig extends GeneralBaseTest {

  SchemaConfig getProperties() throws IOException {
    ProtoBufSchemaConfig config = new ProtoBufSchemaConfig();
    ProtoBufSchemaConfig.ProtobufConfig protobufSchema = new ProtoBufSchemaConfig.ProtobufConfig();
    protobufSchema.setMessageName("Person");
    protobufSchema.setDescriptorValue(getDescriptor());
    config.setProtobufConfig(protobufSchema);
    return config;
  }

  @Override
  SchemaConfig buildConfig() throws IOException {
    ProtoBufSchemaConfig config = new ProtoBufSchemaConfig();
    ProtoBufSchemaConfig.ProtobufConfig protobufSchema = new ProtoBufSchemaConfig.ProtobufConfig();
    protobufSchema.setMessageName("Person");
    protobufSchema.setDescriptorValue(getDescriptor());
    config.setProtobufConfig(protobufSchema);
    setBaseConfig(config);
    return config;
  }

  @Override
  void validate(SchemaConfig schemaConfig) throws IOException {
    Assertions.assertInstanceOf(ProtoBufSchemaConfig.class, schemaConfig);
    ProtoBufSchemaConfig config = (ProtoBufSchemaConfig) schemaConfig;
    ProtoBufSchemaConfig.ProtobufConfig protobufSchema = config.getProtobufConfig();
    Assertions.assertArrayEquals(getDescriptor(), protobufSchema.getDescriptorValue());
    Assertions.assertEquals("Person", protobufSchema.getMessageName());
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
    ProtoBufSchemaConfig.ProtobufConfig protobufSchema = new ProtoBufSchemaConfig.ProtobufConfig();
    protobufSchema.setMessageName("justAName");
    config.setProtobufConfig(protobufSchema);
    config.setUniqueId(UUID.randomUUID());
    config.setExpiresAfter(OffsetDateTime.now().plusDays(10));
    config.setNotBefore(OffsetDateTime.now().minusDays(10));
    Assertions.assertThrowsExactly(IOException.class, config::pack);
  }

  @Test
  void invalidConfigWithDescriptor() {
    ProtoBufSchemaConfig config = new ProtoBufSchemaConfig();
    config.setUniqueId(UUID.randomUUID());
    config.setExpiresAfter(OffsetDateTime.now().plusDays(10));
    config.setNotBefore(OffsetDateTime.now().minusDays(10));
    Assertions.assertThrowsExactly(IOException.class, config::pack);
  }


  @Test
  void invalidConfig() {
    ProtoBufSchemaConfig config = new ProtoBufSchemaConfig();
    config.setUniqueId(UUID.randomUUID());
    config.setExpiresAfter(OffsetDateTime.now().plusDays(10));
    config.setNotBefore(OffsetDateTime.now().minusDays(10));
    Assertions.assertThrowsExactly(IOException.class, config::pack);
  }
}
