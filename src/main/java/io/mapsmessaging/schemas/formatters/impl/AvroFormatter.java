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

package io.mapsmessaging.schemas.formatters.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.AvroSchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.walker.MapResolver;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.*;
import org.apache.avro.util.Utf8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.FORMATTER_UNEXPECTED_OBJECT;

/**
 * The type Avro formatter.
 */
public class AvroFormatter extends MessageFormatter {

  private final DatumReader<GenericRecord> datumReader;
  private final Schema schema;
  private BinaryDecoder decoder;


  /**
   * Instantiates a new Avro formatter.
   */
  public AvroFormatter() {
    datumReader = null;
    schema = null;
    decoder = null;
  }

  /**
   * Instantiates a new Avro formatter.
   *
   * @param schemaDefinition the schema definition
   */
  AvroFormatter(String schemaDefinition) {
    schema = new Schema.Parser().parse(schemaDefinition);
    datumReader = new GenericDatumReader<>(schema);
  }

  @Override
  public synchronized ParsedObject parse(byte[] payload) {
    try {
      decoder = DecoderFactory.get().binaryDecoder(payload, decoder);
      GenericRecord genericRecord = datumReader.read(null, decoder);
      return new AvroResolver(genericRecord);
    } catch (IOException e) {
      logger.log(FORMATTER_UNEXPECTED_OBJECT, getName(), payload);
      return new DefaultParser(payload);
    }
  }

  @Override
  public JsonObject parseToJson(byte[] payload) throws IOException {
    GenericRecord genericRecord = (GenericRecord) parse(payload).getReferenced();
    ByteArrayOutputStream stream = new ByteArrayOutputStream(1024);
    Encoder jsonEncoder = EncoderFactory.get().jsonEncoder(schema, stream);
    GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
    writer.write(genericRecord, jsonEncoder);
    jsonEncoder.flush();
    String jsonString = stream.toString(StandardCharsets.UTF_8);
    return JsonParser.parseString(jsonString).getAsJsonObject();
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

  @Override
  public Map<String, Object> getFormat() {
    if (schema == null || schema.getType() != Schema.Type.RECORD) {
      return Map.of();
    }

    Map<String, Object> result = new java.util.LinkedHashMap<>();

    for (Schema.Field field : schema.getFields()) {
      Map<String, Object> fieldInfo = new java.util.LinkedHashMap<>();
      fieldInfo.put("type", field.schema().getType().getName());
      fieldInfo.put("doc", field.doc());
      fieldInfo.put("default", field.defaultVal());
      result.put(field.name(), fieldInfo);
    }

    return result;
  }


  /**
   * The type Avro resolver.
   */
  public static class AvroResolver implements ParsedObject {

    private final GenericRecord genericRecord;

    /**
     * Instantiates a new Avro resolver.
     *
     * @param genericRecord the generic record
     */
    public AvroResolver(GenericRecord genericRecord) {
      this.genericRecord = genericRecord;
    }

    @Override
    public Object get(String s) {
      String lookup = s;
      boolean isArray = false;
      if (s.endsWith("]")) {
        lookup = s.substring(0, s.indexOf("["));
        isArray = true;
      }
      if (genericRecord.hasField(lookup)) {
        Object val = genericRecord.get(lookup);
        if (val instanceof List && isArray) {
          String index = s.substring(s.indexOf("[") + 1, s.indexOf("]"));
          var idx = Integer.parseInt(index.trim());
          if (((List) val).size() > idx) {
            val = ((List) val).get(idx);
          } else {
            return null;
          }
        }
        if (val instanceof GenericRecord) {
          return new AvroResolver((GenericRecord) val);
        }
        if (val instanceof Map) {
          return new MapResolver((Map) val);
        }
        if (val instanceof Utf8) {
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
