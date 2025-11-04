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


import io.mapsmessaging.schemas.model.XRegistrySchemaVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public abstract class GeneralBaseTest {

  abstract XRegistrySchemaVersion getProperties() throws IOException;

  abstract XRegistrySchemaVersion buildConfig() throws IOException;

  abstract void validate(XRegistrySchemaVersion schemaConfig) throws IOException;

  void setBaseConfig(XRegistrySchemaVersion config) {
    config.setUniqueId(UUID.randomUUID());
    config.setComments("Unit Tests");
    config.setResourceType("sensor");
    config.setInterfaceDescription("Temperature C");

    config.setExpiresAfter(OffsetDateTime.now().plusDays(10));
    config.setNotBefore(OffsetDateTime.now().minusDays(10));
    config.setSource("tcp://localhost:1883/topic2");
  }

  void validateSchema(XRegistrySchemaVersion schemaConfig) throws IOException {
    validate(schemaConfig);
    Assertions.assertEquals("Unit Tests", schemaConfig.getComments());
    Assertions.assertEquals("Temperature C", schemaConfig.getInterfaceDescription());
    Assertions.assertEquals("sensor", schemaConfig.getResourceType());

    Assertions.assertNotNull(schemaConfig.getUniqueId());

    Assertions.assertTrue(schemaConfig.getExpiresAfter().isAfter(OffsetDateTime.now()));
    Assertions.assertTrue(schemaConfig.getNotBefore().isBefore(OffsetDateTime.now()));
    Assertions.assertEquals("tcp://localhost:1883/topic2", schemaConfig.getSource());

  }

  XRegistrySchemaVersion getSchemaProperties() throws IOException {
    XRegistrySchemaVersion properties = getProperties();
    // --- Root-level metadata ---
    String uid = UUID.randomUUID().toString();
    properties.setVersionId(uid);
    properties.setDescription("Unit test schema for temperature readings");
    properties.setComments("Unit Tests");
    properties.setInterfaceDescription("Temperature C");
    properties.setResourceType("sensor");
    properties.setNotBefore(OffsetDateTime.now(ZoneOffset.UTC).minusDays(10));
    properties.setExpiresAfter(OffsetDateTime.now(ZoneOffset.UTC).plusDays(10));
    properties.setSource("tcp://localhost:1883/topic2");
    return properties;
  }


  @Test
  void validateBaseConstructor() throws IOException {
    XRegistrySchemaVersion schemaConfig = buildConfig();
    validate(schemaConfig);
    validateSchema(schemaConfig);
  }


  @Test
  void validateConstructors() throws IOException {
    XRegistrySchemaVersion schemaProps = buildConfig();
    Assertions.assertNotNull(schemaProps);
    validate(schemaProps);
    validateSchema(schemaProps);
  }

  @Test
  void validateStreamConstructors() throws IOException {
    XRegistrySchemaVersion schemaProps = buildConfig();
    XRegistrySchemaVersion schemaConfig = SchemaConfigFactory.getInstance().constructConfig(schemaProps.pack());
    validate(schemaConfig);
    Assertions.assertEquals(schemaProps.getClass().getName(), schemaConfig.getClass().getName());
    validate(schemaConfig);
    Assertions.assertEquals(schemaProps.getFormat(), schemaConfig.getFormat());
  }

  @Test
  void validateConstructorFromMap() throws IOException {
    XRegistrySchemaVersion schemaConfig = buildConfig();
    validate(schemaConfig);
    validateSchema(schemaConfig);
    byte[] schemaProps = schemaConfig.pack();
    XRegistrySchemaVersion schemaConfigCheck = SchemaConfigFactory.getInstance().constructConfig(schemaProps);
    Assertions.assertNotNull(schemaConfigCheck);
    validate(schemaConfigCheck);
    validateSchema(schemaConfigCheck);
  }

}
