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
import io.mapsmessaging.schemas.config.impl.cbc.CrcType;
import io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification;
import io.mapsmessaging.schemas.config.impl.cbc.PrimitiveType;
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
    props.put("littleEndian", true);
    props.put("includeHeaderChecksum", false);
    props.put("checksumType", "NONE");
    props.put("messageTypeId", 0);

    // Minimal valid field list: one unsigned 16-bit field, then byte-align
    List<Map<String, Object>> fields = new ArrayList<>();
    Map<String, Object> f1 = new LinkedHashMap<>();
    f1.put("fieldName", "sensorId");
    f1.put("primitiveType", "UNSIGNED_INTEGER");
    f1.put("bitWidth", 16);
    f1.put("signed", false);
    f1.put("byteAlignAfter", true);
    fields.add(f1);

    // Second field: signed 12-bit temperature with scale/offset
    Map<String, Object> f2 = new LinkedHashMap<>();
    f2.put("fieldName", "temperatureC");
    f2.put("primitiveType", "SIGNED_INTEGER");
    f2.put("bitWidth", 12);
    f2.put("signed", true);
    f2.put("scale", 0.1d);
    f2.put("offset", 0.0d);
    fields.add(f2);

    props.put("fields", fields);
    return props;
  }

  @Override
  SchemaConfig buildConfig() throws IOException {
    CbcSchemaConfig config = new CbcSchemaConfig();
    setBaseConfig(config);
    // CBC defaults
    config.setLittleEndian(true);
    config.setIncludeHeaderChecksum(false);
    config.setChecksumType(CrcType.NONE);
    config.setMessageTypeId(0);

    // Mirror the field list used in getProperties()
    List<FieldSpecification> list = new ArrayList<>();

    FieldSpecification sensorId =
        FieldSpecification.builder()
            .fieldName("sensorId")
            .primitiveType(PrimitiveType.UNSIGNED_INTEGER)
            .bitWidth(16)
            .signed(false)
            .byteAlignAfter(true)
            .build();
    list.add(sensorId);

    FieldSpecification temp =
        FieldSpecification.builder()
            .fieldName("temperatureC")
            .primitiveType(PrimitiveType.SIGNED_INTEGER)
            .bitWidth(12)
            .signed(true)
            .scale(0.1d)
            .offset(0.0d)
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
    Assertions.assertTrue(c.isLittleEndian());
    Assertions.assertEquals(CrcType.NONE, c.getChecksumType());
    Assertions.assertNotNull(c.getFieldSpecificationList());
    Assertions.assertFalse(c.getFieldSpecificationList().isEmpty());
    Assertions.assertEquals("sensorId", c.getFieldSpecificationList().get(0).getFieldName());
  }
}
