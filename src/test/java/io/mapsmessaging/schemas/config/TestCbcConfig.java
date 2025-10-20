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
import io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for CBC SchemaConfig using GeneralBaseTest harness.
 */
class TestCbcConfig extends GeneralBaseTest {

  @Override
  Map<String, Object> getProperties() {
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("format", "cbc");                       // must match CbcSchemaConfig.TYPE
    props.put("mimeType", "application/x-cbc");

    // Optional CBC-level settings
    props.put("message", 0);

    // Minimal valid field list: one unsigned 16-bit field, then byte-align
    List<Map<String, Object>> fields = new ArrayList<>();
    Map<String, Object> f1 = new LinkedHashMap<>();
    f1.put("name", "sensorId");
    f1.put("type", "uint");
    f1.put("size", 16);
    fields.add(f1);

    // Second field: signed 12-bit temperature with scale/offset
    Map<String, Object> f2 = new LinkedHashMap<>();
    f2.put("name", "temperatureC");
    f2.put("type", "int");
    f2.put("size", 12);
    fields.add(f2);

    props.put("fields", fields);
    return props;
  }

  @Override
  SchemaConfig buildConfig() throws IOException {
    CbcSchemaConfig config = new CbcSchemaConfig();
    setBaseConfig(config);
    // CBC defaults
    config.setMessageKey(0);

    // Mirror the field list used in getProperties()
    List<FieldSpecification> list = new ArrayList<>();

    FieldSpecification sensorId =
        FieldSpecification.builder()
            .name("sensorId")
            .type("uint")
            .size(16)
            .build();
    list.add(sensorId);

    FieldSpecification temp =
        FieldSpecification.builder()
            .name("temperatureC")
            .type("uint")
            .size(12)
            .build();
    list.add(temp);

    config.setFieldSpecificationList(list);
    config.setMimeType("application/x-cbc");
    return config;
  }

  @Override
  void validate(SchemaConfig schemaConfig) {
    Assertions.assertInstanceOf(CbcSchemaConfig.class, schemaConfig);
    CbcSchemaConfig c = (CbcSchemaConfig) schemaConfig;
    Assertions.assertEquals("application/x-cbc", c.getMimeType());
    Assertions.assertNotNull(c.getFieldSpecificationList());
    Assertions.assertFalse(c.getFieldSpecificationList().isEmpty());
    Assertions.assertEquals("sensorId", c.getFieldSpecificationList().get(0).getName());
  }
}
