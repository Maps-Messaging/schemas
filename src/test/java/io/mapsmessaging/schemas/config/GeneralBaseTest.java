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
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class GeneralBaseTest {

  abstract Map<String, Object> getProperties();

  abstract SchemaConfig buildConfig();

  abstract void validate(SchemaConfig schemaConfig);

  void validateSchema(SchemaConfig schemaConfig) {
    validate(schemaConfig);
    Assertions.assertTrue(schemaConfig.getExpiresAfter().isAfter(LocalDateTime.now()));
    Assertions.assertTrue(schemaConfig.getNotBefore().isBefore(LocalDateTime.now()));
    Assertions.assertNotNull(schemaConfig.getUniqueId());
  }

  Map<String, Object> getSchemaProperties() {
    Map<String, Object> schema = new LinkedHashMap<>();
    Map<String, Object> props = getProperties();
    props.put("uuid", UUID.randomUUID());
    props.put("notBefore", LocalDateTime.now().minusDays(10));
    props.put("expiresAfter", LocalDateTime.now().plusDays(10));
    schema.put("schema", props);
    return schema;
  }

  @Test
  void validateBaseConstructor() {
    SchemaConfig schemaConfig = buildConfig();
    validate(schemaConfig);
    validateSchema(schemaConfig);
  }


  @Test
  void validateConstructors() throws IOException {
    Map<String, Object> schemaProps = getSchemaProperties();
    String format = ((Map<String, Object>) schemaProps.get("schema")).get("format").toString();
    SchemaConfig schemaConfig = SchemaConfigFactory.getInstance().constructConfig(schemaProps);
    Assertions.assertEquals(format, schemaConfig.getFormat());
    validate(schemaConfig);
    validateSchema(schemaConfig);
  }

  @Test
  void validateStreamConstructors() throws IOException {
    Map<String, Object> schemaProps = getSchemaProperties();
    String format = ((Map<String, Object>) schemaProps.get("schema")).get("format").toString();
    SchemaConfig schemaConfig = SchemaConfigFactory.getInstance().constructConfig(schemaProps);
    validate(schemaConfig);
    Assertions.assertEquals(format, schemaConfig.getFormat());
    String packed = schemaConfig.pack();
    SchemaConfig parsed = SchemaConfigFactory.getInstance().constructConfig(packed);
    validate(parsed);
    Assertions.assertEquals(format, parsed.getFormat());
  }

  @Test
  void validateConstructorFromMap() throws IOException {
    Map<String, Object> schemaProps = getSchemaProperties();
    String format = ((Map<String, Object>) schemaProps.get("schema")).get("format").toString();
    SchemaConfig schemaConfig = SchemaConfigFactory.getInstance().constructConfig(schemaProps);
    Assertions.assertEquals(format, schemaConfig.getFormat());
    validate(schemaConfig);
    validateSchema(schemaConfig);
    schemaProps = schemaConfig.toMap();
    SchemaConfig schemaConfigCheck = SchemaConfigFactory.getInstance().constructConfig(schemaProps);
    Assertions.assertNotNull(schemaConfigCheck);
    validate(schemaConfigCheck);
    validateSchema(schemaConfigCheck);
  }

}
