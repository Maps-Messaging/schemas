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

import static io.mapsmessaging.schemas.config.TestAvro.getSchema;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.SchemaConfigFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestAvro {

  @Test
  void testAvro() throws IOException {
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("format", "AVRO");
    props.put("schema", new String(Base64.getEncoder().encode(getSchema())));
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("schema", props);
    SchemaConfig config = SchemaConfigFactory.getInstance().constructConfig(schema);
    Assertions.assertEquals("AVRO", config.getFormat());
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(config);
    Assertions.assertEquals("AVRO", formatter.getName());

    PersonAvro e1 = new PersonAvro();
    e1.setName("Matthew Buckton");;
    e1.setId(2);
    e1.setEmail("admin@gmail.com");
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    byte[] data;
    Encoder binaryEncoder = null;
    binaryEncoder = EncoderFactory.get().binaryEncoder(stream, null);
    DatumWriter<PersonAvro> writer = new SpecificDatumWriter<>(PersonAvro.class);
    writer.write(e1, binaryEncoder);
    binaryEncoder.flush();
    data = stream.toByteArray();

    ParsedObject obj = formatter.parse(data);
    Assertions.assertEquals("Matthew Buckton", obj.get("name").toString());
    Assertions.assertEquals(2, obj.get("id"));
    Assertions.assertEquals("admin@gmail.com", obj.get("email"));
  }

  @Test
  void testAvroToJson() throws IOException {
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("format", "AVRO");
    props.put("schema", new String(Base64.getEncoder().encode(getSchema())));
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("schema", props);
    SchemaConfig config = SchemaConfigFactory.getInstance().constructConfig(schema);
    Assertions.assertEquals("AVRO", config.getFormat());
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(config);
    Assertions.assertEquals("AVRO", formatter.getName());

    PersonAvro e1 = new PersonAvro();
    e1.setName("Matthew Buckton");;
    e1.setId(2);
    e1.setEmail("admin@gmail.com");
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    byte[] data;
    Encoder binaryEncoder = null;
    binaryEncoder = EncoderFactory.get().binaryEncoder(stream, null);
    DatumWriter<PersonAvro> writer = new SpecificDatumWriter<>(PersonAvro.class);
    writer.write(e1, binaryEncoder);
    binaryEncoder.flush();
    data = stream.toByteArray();
    JSONObject jsonObject = formatter.parseToJson(data);

    Assertions.assertEquals("Matthew Buckton", jsonObject.get("name").toString());
    Assertions.assertEquals(2, jsonObject.get("id"));
    Assertions.assertEquals("admin@gmail.com", jsonObject.get("email"));
  }
}
