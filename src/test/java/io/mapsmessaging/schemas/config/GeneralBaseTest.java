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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class GeneralBaseTest {

  abstract Map<String, Object> getProperties();

  Map<String, Object>  getSchemaProperties(){
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("schema", getProperties());
    return schema;
  }

  @Test
  void validateConstructors() throws IOException {
    Map<String, Object> schemaProps = getSchemaProperties();
    String format = ((Map<String, Object>)schemaProps.get("schema")).get("format").toString();
    SchemaConfig schemaConfig = SchemaConfigFactory.getInstance().constructConfig(schemaProps);
    Assertions.assertEquals(format, schemaConfig.getFormat());
  }

  @Test
  void validateStreamConstructors() throws IOException {
    Map<String, Object> schemaProps = getSchemaProperties();
    String format = ((Map<String, Object>)schemaProps.get("schema")).get("format").toString();
    SchemaConfig schemaConfig = SchemaConfigFactory.getInstance().constructConfig(schemaProps);
    Assertions.assertEquals(format, schemaConfig.getFormat());
    String packed = schemaConfig.pack();
    SchemaConfig parsed = SchemaConfigFactory.getInstance().constructConfig(packed);
    Assertions.assertEquals(format, parsed.getFormat());
  }

}
