
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
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestProtobuf {


  @Test
  void protobufTest() throws IOException {
    byte[] descriptor = getDescriptor();
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("format", "ProtoBuf");
    props.put("descriptor", new String(Base64.getEncoder().encode(descriptor)));
    props.put("messageName", "Person");
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("schema", props);
    SchemaConfig config = SchemaConfigFactory.getInstance().constructConfig(schema);

    Assertions.assertEquals("ProtoBuf", config.getFormat());
    Assertions.assertArrayEquals(descriptor, ((ProtoBufSchemaConfig)config).getDescriptor());

    JSONObject jsonObject = new JSONObject(config.pack());
    Assertions.assertEquals("ProtoBuf", jsonObject.getJSONObject("schema").get("format"));

    String base64Encoded = new String(Base64.getEncoder().encode(descriptor));
    Assertions.assertEquals(base64Encoded, jsonObject.getJSONObject("schema").get("descriptor"));
  }

  @Test
  void testSchemaReload() throws IOException {
    byte[] descriptor = getDescriptor();

    Map<String, Object> props = new LinkedHashMap<>();
    props.put("format", "ProtoBuf");
    props.put("descriptor", new String(Base64.getEncoder().encode(descriptor)));
    props.put("messageName", "Person");
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("schema", props);
    SchemaConfig config = SchemaConfigFactory.getInstance().constructConfig(schema);

    Assertions.assertEquals("ProtoBuf", config.getFormat());
    String packed = config.pack();
    SchemaConfig parsed = SchemaConfigFactory.getInstance().constructConfig(packed);
    Assertions.assertEquals("ProtoBuf", parsed.getFormat());
  }

  private byte[] getDescriptor() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(10240);
    byte[] tmp = new byte[10240];
    try (InputStream fis = TestProtobuf.class.getClassLoader().getResourceAsStream("Person.desc")) {
      int len = fis.read(tmp);
      baos.write(tmp, 0, len);
    }
    return baos.toByteArray();
  }
}
