/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package io.mapsmessaging.schemas.config;

import com.google.gson.JsonParser;
import io.mapsmessaging.schemas.config.impl.CdrSchemaConfig;
import org.junit.jupiter.api.Assertions;

/** Runs the shared SchemaConfig serialization, constructor and factory tests for CDR. */
class TestCdrConfig extends GeneralBaseTest {

  @Override
  SchemaConfig getProperties() {
    return buildConfig();
  }

  @Override
  SchemaConfig buildConfig() {
    CdrSchemaConfig config = new CdrSchemaConfig();
    setBaseConfig(config);
    config.setSchema(JsonParser.parseString("""
        {"fields":[
          {"name":"stringId","type":"string"},
          {"name":"longId","type":"int64"},
          {"name":"intId","type":"int32"},
          {"name":"floatId","type":"float32"},
          {"name":"doubleId","type":"float64"}
        ]}
        """).getAsJsonObject());
    return config;
  }

  @Override
  void validate(SchemaConfig schemaConfig) {
    Assertions.assertInstanceOf(CdrSchemaConfig.class, schemaConfig);
    Assertions.assertEquals("cdr", schemaConfig.getFormat());
    Assertions.assertNotNull(schemaConfig.getSchema());
    Assertions.assertEquals(5, schemaConfig.getSchema().getAsJsonArray("fields").size());
  }
}
