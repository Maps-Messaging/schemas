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

import static io.mapsmessaging.schemas.config.TestAvro.AVRO_SCHEMA;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.SchemaConfigFactory;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestAvro {
  @Test
  void testAvro() throws IOException {
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("format", "AVRO");
    props.put("schema", new String(Base64.getEncoder().encode(AVRO_SCHEMA.getBytes())));
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("schema", props);
    SchemaConfig config = SchemaConfigFactory.getInstance().constructConfig(schema);
    Assertions.assertEquals("AVRO", config.getFormat());
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(config);
    Assertions.assertEquals("AVRO", formatter.getName());
  }
}
