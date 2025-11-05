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

package io.mapsmessaging.schemas.formatters;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.TestAvroConfig;
import io.mapsmessaging.schemas.config.impl.AvroSchemaConfig;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class TestAvroFormatter extends BaseTest {

  byte[] pack(io.mapsmessaging.schemas.formatters.Person p) throws IOException {
    PersonAvro personA = new PersonAvro();
    personA.setStringId(p.getStringId());
    personA.setIntId(p.getIntId());
    personA.setLongId(p.getLongId());
    personA.setFloatId(p.getFloatId());
    personA.setDoubleId(p.getDoubleId());
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Encoder binaryEncoder = EncoderFactory.get().binaryEncoder(stream, null);
    DatumWriter<PersonAvro> writer = new SpecificDatumWriter<>(PersonAvro.class);
    writer.write(personA, binaryEncoder);
    binaryEncoder.flush();
    return stream.toByteArray();
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
    AvroSchemaConfig avroSchemaConfig = new AvroSchemaConfig();
    avroSchemaConfig.setSchema(TestAvroConfig.getSchema());
    return avroSchemaConfig;
  }
}
