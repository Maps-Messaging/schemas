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
import io.mapsmessaging.schemas.config.SchemaConfigFactory;
import io.mapsmessaging.schemas.config.impl.AvroSchemaConfig;
import io.mapsmessaging.schemas.formatters.PersonProto.Person.Builder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestAvro extends BaseTest {

  byte[] pack(io.mapsmessaging.schemas.formatters.Person p) throws IOException {
    PersonAvro e1 = new PersonAvro();
    e1.setName(p.getName());;
    e1.setId(p.getId());
    e1.setEmail(p.getEmail());
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Encoder binaryEncoder = EncoderFactory.get().binaryEncoder(stream, null);
    DatumWriter<PersonAvro> writer = new SpecificDatumWriter<>(PersonAvro.class);
    writer.write(e1, binaryEncoder);
    binaryEncoder.flush();
    return stream.toByteArray();
  }


  @Override
  List<byte[]> packList(List<io.mapsmessaging.schemas.formatters.Person> list) throws IOException {
    List<byte[]> packed = new ArrayList<>();
    for(io.mapsmessaging.schemas.formatters.Person p:list){
      packed.add(pack(p));
    }
    return packed;
  }

  @Override
  SchemaConfig getSchema() throws IOException {
    AvroSchemaConfig avroSchemaConfig = new AvroSchemaConfig();
    avroSchemaConfig.setSchema(io.mapsmessaging.schemas.config.TestAvro.getSchema());
    return avroSchemaConfig;
  }
}
