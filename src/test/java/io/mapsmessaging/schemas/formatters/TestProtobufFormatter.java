
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
package io.mapsmessaging.schemas.formatters;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.ProtoBufSchemaConfig;
import io.mapsmessaging.schemas.formatters.PersonProto.Person.Builder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

class TestProtobufFormatter extends BaseTest {

  private byte[] pack(io.mapsmessaging.schemas.formatters.Person p) throws IOException {
    Builder builder = PersonProto.Person.newBuilder();
    builder.setStringId(p.getStringId());
    builder.setIntId(p.getIntId());
    builder.setLongId(p.getLongId());
    builder.setFloatId(p.getFloatId());
    builder.setDoubleId(p.getDoubleId());
    PersonProto.Person person = builder.build();
    ByteArrayOutputStream baos = new ByteArrayOutputStream(10240);
    person.writeTo(baos);
    return baos.toByteArray();
  }

  @Override
  List<byte[]> packList(List<io.mapsmessaging.schemas.formatters.Person> list) throws IOException {
    List<byte[]> packed = new ArrayList<>();
    for (io.mapsmessaging.schemas.formatters.Person p : list) {
      packed.add(pack(p));
    }
    return packed;
  }

  @Override
  SchemaConfig getSchema() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(10240);
    byte[] tmp = new byte[10240];
    try (InputStream fis = getClass().getClassLoader().getResourceAsStream("Person.desc")) {
      int len = fis.read(tmp);
      baos.write(tmp, 0, len);
    }
    ProtoBufSchemaConfig config = new ProtoBufSchemaConfig();
    config.setDescriptorValue(baos.toByteArray());
    config.setMessageName("Person");
    return config;
  }
}
