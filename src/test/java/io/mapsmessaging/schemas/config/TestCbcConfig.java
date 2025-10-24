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

import io.mapsmessaging.schemas.config.impl.CbcSchemaConfig;
import io.mapsmessaging.schemas.config.impl.cbc.CbcFormat;
import io.mapsmessaging.schemas.model.XRegistrySchemaVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static io.mapsmessaging.schemas.formatters.cbc.TestCbcHeartbeatConformance.INMARSAT_HEARTBEAT_JSON;

/**
 * Tests for CBC SchemaConfig using GeneralBaseTest harness.
 */
class TestCbcConfig extends GeneralBaseTest {

  @Override
  XRegistrySchemaVersion getProperties() {
    CbcSchemaConfig cbcSchemaConfig = new CbcSchemaConfig();
    cbcSchemaConfig.setSchema(cbcSchema);
    return cbcSchemaConfig;
  }

  @Override
  XRegistrySchemaVersion buildConfig() throws IOException {
    CbcSchemaConfig cbcSchemaConfig = new CbcSchemaConfig();
    cbcSchemaConfig.setSchema(cbcSchema);
    setBaseConfig(cbcSchemaConfig);
    return cbcSchemaConfig;
  }

  @Override
  void validate(XRegistrySchemaVersion schemaConfig) {
    Assertions.assertInstanceOf(CbcSchemaConfig.class, schemaConfig);
    CbcSchemaConfig c = (CbcSchemaConfig) schemaConfig;
    Assertions.assertEquals("application/x-cbc", c.getMimeType());
    CbcFormat cbcFormat = c.getCbcFormat();
    Assertions.assertNotNull(cbcFormat.getFields());
    Assertions.assertFalse(cbcFormat.getFields().isEmpty());
    Assertions.assertEquals("sensorId", cbcFormat.getFields().get(0).getName());
  }

  @Test
  void testCompleteParseToList() throws IOException {
    List<CbcSchemaConfig> configList = CbcSchemaConfig.parseSchema(INMARSAT_HEARTBEAT_JSON);
    Assertions.assertNotNull(configList);
    Assertions.assertFalse(configList.isEmpty());
    for (CbcSchemaConfig config : configList) {
      String packed = new String(config.pack());
      Assertions.assertNotNull(packed);
      XRegistrySchemaVersion reloaded = SchemaConfigFactory.getInstance().constructConfig(packed);
      Assertions.assertNotNull(reloaded);
      Assertions.assertInstanceOf(CbcSchemaConfig.class, reloaded);
    }
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
