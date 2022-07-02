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

package io.mapsmessaging.schemas.formatters.impl;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.AvroSchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.walker.MapResolver;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.util.Utf8;
import org.json.JSONObject;

public class AvroFormatter implements MessageFormatter {

  private final DatumReader<GenericRecord> datumReader;
  private final Schema schema;
  private BinaryDecoder decoder;


  public AvroFormatter() {
    datumReader = null;
    schema = null;
    decoder = null;
  }

  AvroFormatter(String schemaDefinition) {
    schema = new Schema.Parser().parse(schemaDefinition);
    datumReader = new GenericDatumReader<>(schema);
  }

  @Override
  public synchronized ParsedObject parse(byte[] payload){
    try {
      decoder = DecoderFactory.get().binaryDecoder(payload, decoder);
      GenericRecord genericRecord = datumReader.read(null, decoder);
      return new AvroResolver(genericRecord);
    } catch (IOException e) {

    }
    return null;
  }

  @Override
  public JSONObject parseToJson(byte[] payload) throws IOException {
    GenericRecord genericRecord = (GenericRecord) parse(payload).getReferenced();
    ByteArrayOutputStream stream = new ByteArrayOutputStream(1024);
    Encoder binaryEncoder = EncoderFactory.get().jsonEncoder(schema, stream);
    GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
    writer.write(genericRecord, binaryEncoder);
    binaryEncoder.flush();
    return new JSONObject(stream.toString());
  }

  @Override
  public byte[] pack(Object object) throws IOException {
    String toPack = null;
    if (object instanceof String) {
      toPack = (String) object;
    }
    if (object instanceof JSONObject) {
      toPack = ((JSONObject) object).toString(2);
    }
    if (toPack != null) {
      return toPack.getBytes(StandardCharsets.UTF_8);
    }
    throw new IOException("Unexpected object to be packed");
  }


  @Override
  public MessageFormatter getInstance(SchemaConfig config) throws IOException {
    AvroSchemaConfig avroSchemaConfig = (AvroSchemaConfig) config;
    return new AvroFormatter(avroSchemaConfig.getSchema());
  }

  @Override
  public String getName() {
    return "AVRO";
  }


  public static class AvroResolver implements ParsedObject {

    private final GenericRecord genericRecord;

    public AvroResolver(GenericRecord genericRecord){
      this.genericRecord = genericRecord;
    }

    @Override
    public Object get(String s) {
      String lookup = s;
      boolean isArray = false;
      if(s.endsWith("]")){
        lookup = s.substring(0, s.indexOf("["));
        isArray = true;
      }
      if(genericRecord.hasField(lookup)) {
        Object val = genericRecord.get(lookup);
        if(val instanceof List && isArray){
          String index = s.substring(s.indexOf("[")+1, s.indexOf("]"));
          var idx = Integer.parseInt(index.trim());
          if( ( (List)val).size() > idx) {
            val = ((List) val).get(idx);
          }
          else{
            return null;
          }
        }
        if(val instanceof GenericRecord){
          return new AvroResolver((GenericRecord) val);
        }
        if(val instanceof Map){
          return new MapResolver((Map)val);
        }
        if(val instanceof Utf8){
          return val.toString();
        }
        return val;
      }
      return null;
    }

    @Override
    public Object getReferenced() {
      return genericRecord;
    }
  }

}
