/*
 *
 *     Copyright [ 2020 - 2026 ] [Matthew Buckton]
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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.schemas.config.impl.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConfigHelper {

  public static SchemaConfig buildAvroConfig() throws IOException {
    AvroSchemaConfig config = new AvroSchemaConfig();
    ByteArrayOutputStream baos = new ByteArrayOutputStream(10240);
    byte[] tmp = new byte[10240];
    try (InputStream fis = TestProtobufConfig.class.getClassLoader().getResourceAsStream("avro/Person.avsc")) {
      int len = fis.read(tmp);
      baos.write(tmp, 0, len);
    }
    JsonObject schema = JsonParser.parseString(baos.toString()).getAsJsonObject();
    config.setSchema(schema);
    setBaseConfig(config);
    return config;
  }


  public static SchemaConfig buildCbcConfig() throws IOException {
    CbcSchemaConfig cbcSchemaConfig = new CbcSchemaConfig();
    cbcSchemaConfig.setSchema(cbcSchema);
    setBaseConfig(cbcSchemaConfig);
    return cbcSchemaConfig;
  }


  public static SchemaConfig buildCborConfig() {
    CborSchemaConfig config = new CborSchemaConfig();
    setBaseConfig(config);
    JsonObject obj = JsonParser.parseString(cborSchema).getAsJsonObject();
    config.setSchema(obj);
    return config;
  }

  public static SchemaConfig buildCsvConfig() {
    CsvSchemaConfig config = new CsvSchemaConfig();
    CsvSchemaConfig.CsvConfig csvConfig = new CsvSchemaConfig.CsvConfig();
    csvConfig.setHeaderValues("name, id, email");
    config.setConfig(csvConfig);
    setBaseConfig(config);
    return config;
  }

  public static SchemaConfig buildJsonConfig() {
    JsonSchemaConfig config = new JsonSchemaConfig();
    setBaseConfig(config);
    return config;
  }


  public static SchemaConfig buildMavlinkConfig() {
    MavlinkSchemaConfig config = new MavlinkSchemaConfig();
    config.setDialect("common");
    setBaseConfig(config);
    return config;
  }

  public static SchemaConfig buildN2KConfig() {
    CanbusSchemaConfig config = new CanbusSchemaConfig();
    config.setXmlPath("NMEA_database_1_to_300.xml");
    setBaseConfig(config);
    return config;
  }

  public static SchemaConfig buildMessagePackConfig() {
    MessagePackSchemaConfig config = new MessagePackSchemaConfig();
    setBaseConfig(config);
    return config;
  }

  public static SchemaConfig buildNativeConfig() {
    NativeSchemaConfig props = new NativeSchemaConfig();
    props.setType(NativeSchemaConfig.TYPE.DOUBLE);
    setBaseConfig(props);
    return props;
  }

  public static SchemaConfig buildProtobufConfig() throws IOException {
    ProtoBufSchemaConfig config = new ProtoBufSchemaConfig();
    ProtoBufSchemaConfig.ProtobufConfig protobufSchema = new ProtoBufSchemaConfig.ProtobufConfig();
    protobufSchema.setMessageName("Person");
    protobufSchema.setDescriptorValue(getDescriptor());
    config.setProtobufConfig(protobufSchema);
    setBaseConfig(config);
    return config;
  }

  public static SchemaConfig buildRawConfig() {
    RawSchemaConfig config = new RawSchemaConfig();
    setBaseConfig(config);
    return config;
  }

  public static SchemaConfig buildXmlConfig() {
    XmlSchemaConfig config = new XmlSchemaConfig();
    XmlSchemaConfig.XmlConfig xmlConfig = new XmlSchemaConfig.XmlConfig();
    xmlConfig.setValidating(true);
    xmlConfig.setCoalescing(true);
    xmlConfig.setNamespaceAware(true);
    config.setConfig(xmlConfig);
    setBaseConfig(config);
    return config;
  }

  public static List<SchemaConfig> getAll() throws IOException {
    List<SchemaConfig> all = new ArrayList<>();
    all.add(buildAvroConfig());
    all.add(buildCbcConfig());
    all.add(buildCborConfig());
    all.add(buildCsvConfig());
    all.add(buildJsonConfig());
    all.add(buildMessagePackConfig());
    all.add(buildNativeConfig());
    all.add(buildProtobufConfig());
    all.add(buildRawConfig());
    all.add(buildXmlConfig());
    all.add(buildMavlinkConfig());
    all.add(buildN2KConfig());
    return all;
  }

  private static byte[] getDescriptor() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(10240);
    byte[] tmp = new byte[10240];
    try (InputStream fis = TestProtobufConfig.class.getClassLoader().getResourceAsStream("Person.desc")) {
      int len = fis.read(tmp);
      baos.write(tmp, 0, len);
    }
    return baos.toByteArray();
  }


  private static final String cborSchema = "{\n" +
      "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
      "  \"type\": \"object\",\n" +
      "  \"properties\": {\n" +
      "    \"stringId\": { \"type\": \"string\" },\n" +
      "    \"longId\": { \"type\": \"number\" },\n" +
      "    \"intId\": { \"type\": \"number\" },\n" +
      "    \"floatId\": { \"type\": \"number\" },\n" +
      "    \"doubleId\": { \"type\": \"number\" }\n" +
      "  },\n" +
      " \"required\": [\"stringId\", \"longId\", \"intId\", \"floatId\", \"doubleId\"],\n" +
      "  \"additionalProperties\": false\n" +
      "}";


  static private void setBaseConfig(SchemaConfig config) {
    config.setUniqueId(UUID.randomUUID());
    config.setComments("Unit Tests");
    config.setResourceType("sensor");
    config.setInterfaceDescription("Temperature C");

    config.setExpiresAfter(OffsetDateTime.now().plusDays(10));
    config.setNotBefore(OffsetDateTime.now().minusDays(10));
    config.setSource("tcp://localhost:1883/topic2");
    config.setVersion(config.getFormat() + "-1.0.0");
  }


  private static final String cbcSchema = "{\n" +
      "    \"messageKey\": 0,\n" +
      "    \"fields\": [\n" +
      "      {\n" +
      "        \"name\": \"sensorId\",\n" +
      "        \"type\": \"uint\",\n" +
      "        \"size\": 16\n" +
      "      },\n" +
      "      {\n" +
      "        \"name\": \"temperatureC\",\n" +
      "        \"type\": \"int\",\n" +
      "        \"size\": 12\n" +
      "      }\n" +
      "    ]\n" +
      "}";
}
