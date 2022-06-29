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

package io.mapsmessaging.schemas.config;

import io.mapsmessaging.schemas.config.impl.CsvSchemaConfig;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;

class TestCsv extends GeneralBaseTest {

  Map<String, Object> getProperties(){
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("format", "CSV");
    props.put("header", "name, id, email");
    return props;
  }

  @Override
  void validate(SchemaConfig schemaConfig) {
    Assertions.assertTrue(schemaConfig instanceof CsvSchemaConfig);
    CsvSchemaConfig config = (CsvSchemaConfig) schemaConfig;
    Assertions.assertEquals("name, id, email", config.getHeader());
  }
}