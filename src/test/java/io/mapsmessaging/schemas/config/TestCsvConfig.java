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

import io.mapsmessaging.schemas.config.impl.CsvSchemaConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

class TestCsvConfig extends GeneralBaseTest {

  SchemaConfig getProperties() {
    CsvSchemaConfig config = new CsvSchemaConfig();
    CsvSchemaConfig.CsvConfig csvConfig = new CsvSchemaConfig.CsvConfig();
    csvConfig.setHeaderValues("name, id, email");
    return config;
  }

  @Override
  SchemaConfig buildConfig() {
    CsvSchemaConfig config = new CsvSchemaConfig();
    CsvSchemaConfig.CsvConfig csvConfig = new CsvSchemaConfig.CsvConfig();
    csvConfig.setHeaderValues("name, id, email");
    config.setConfig(csvConfig);
    setBaseConfig(config);
    return config;
  }

  @Override
  void validate(SchemaConfig schemaConfig) {
    Assertions.assertInstanceOf(CsvSchemaConfig.class, schemaConfig);
    CsvSchemaConfig config = (CsvSchemaConfig) schemaConfig;
    Assertions.assertEquals("name, id, email", config.getConfig().getHeaderValues());
  }

  @Test
  void invalidConfig() {
    CsvSchemaConfig config = new CsvSchemaConfig();
    config.setUniqueId(UUID.randomUUID());
    config.setExpiresAfter(OffsetDateTime.now().plusDays(10));
    config.setNotBefore(OffsetDateTime.now().minusDays(10));
    Assertions.assertThrowsExactly(IOException.class, config::pack);
  }
}