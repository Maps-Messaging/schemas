
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
import java.io.InputStream;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;

class TestProtobufConfig extends GeneralBaseTest {

  Map<String, Object> getProperties() {
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("format", "ProtoBuf");
    props.put("descriptor", new String(Base64.getEncoder().encode(getDescriptor())));
    props.put("messageName", "Person");
    return props;
  }

  @Override
  void validate(SchemaConfig schemaConfig) {
    Assertions.assertTrue(schemaConfig instanceof ProtoBufSchemaConfig);
    ProtoBufSchemaConfig config = (ProtoBufSchemaConfig) schemaConfig;
    Assertions.assertArrayEquals(getDescriptor(), config.getDescriptorValue());
    Assertions.assertEquals("Person", config.getMessageName());
  }


  private byte[] getDescriptor() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(10240);
    byte[] tmp = new byte[10240];
    try (InputStream fis = TestProtobufConfig.class.getClassLoader().getResourceAsStream("Person.desc")) {
      int len = fis.read(tmp);
      baos.write(tmp, 0, len);
    } catch (Exception ex) {
    }
    return baos.toByteArray();
  }


}
