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

package io.mapsmessaging.schemas.repository;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.JsonSchemaConfig;
import io.mapsmessaging.schemas.config.impl.XmlSchemaConfig;
import io.mapsmessaging.schemas.repository.impl.SimpleSchemaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

class TestSimpleRepository {
  @SuppressWarnings("java:S112") // Is thrown by extending classes
  protected SimpleSchemaRepository getRepository() throws IOException {
    return new SimpleSchemaRepository();
  }

  @Test
  void simpleAccess() throws IOException {
    SimpleSchemaRepository repository = getRepository();
    XmlSchemaConfig xml = new XmlSchemaConfig();
    xml.setUniqueId(UUID.randomUUID());
    repository.addSchema("/root", xml);
    Assertions.assertNotNull(repository.getSchema(xml.getUniqueId()));
    Assertions.assertEquals(xml, repository.getSchema(xml.getUniqueId()));

    Assertions.assertNotNull(repository.getSchemaByContext("/root"));
    Assertions.assertEquals(xml, repository.getSchemaByContext("/root").get(0));

    repository.removeAllSchemas();
    Assertions.assertNull(repository.getSchema("/root"));
    Assertions.assertNull(repository.getSchema(xml.getUniqueId()));

    repository.removeAllSchemas();
    Assertions.assertEquals(0, repository.getAll().size());
  }

  @Test
  void simpleQueryAccess() throws IOException {
    SimpleSchemaRepository repository = getRepository();
    for (int x = 0; x < 10; x++) {
      XmlSchemaConfig xml = new XmlSchemaConfig();
      xml.setUniqueId(UUID.randomUUID());
      repository.addSchema("/root/xml/" + x, xml);
      Assertions.assertNotNull(repository.getSchema(xml.getUniqueId()));
      Assertions.assertEquals(xml, repository.getSchema(xml.getUniqueId()));

      Assertions.assertNotNull(repository.getSchemaByContext("/root/xml/" + x));
      Assertions.assertEquals(xml, repository.getSchemaByContext("/root/xml/" + x).get(0));
    }
    for(int x=0;x<10;x++) {
      JsonSchemaConfig json = new JsonSchemaConfig();
      json.setUniqueId(UUID.randomUUID());
      repository.addSchema("/root/json/" + x, json);
      Assertions.assertNotNull(repository.getSchema(json.getUniqueId()));
      Assertions.assertEquals(json, repository.getSchema(json.getUniqueId()));

      Assertions.assertNotNull(repository.getSchemaByContext("/root/json/" + x));
      Assertions.assertEquals(json, repository.getSchemaByContext("/root/json/" + x).get(0));
    }
    Assertions.assertEquals(10, repository.getSchemas("json").size());
    Assertions.assertEquals(10, repository.getSchemas("xml").size());
    Assertions.assertEquals(20, repository.getAll().size());

    for(SchemaConfig config: repository.getSchemas("xml")){
      repository.removeSchema(config.getUniqueId());
    }
    Assertions.assertEquals(10, repository.getSchemas("json").size());
    Assertions.assertEquals(0, repository.getSchemas("xml").size());

    for (SchemaConfig config : repository.getSchemas("json")) {
      repository.removeSchema(config.getUniqueId());
    }
    Assertions.assertEquals(0, repository.getSchemas("json").size());
    Assertions.assertEquals(0, repository.getSchemas("xml").size());
    repository.removeAllSchemas();
    Assertions.assertEquals(0, repository.getAll().size());

  }

  @Test
  void simpleAddRemoveAccess() throws IOException {
    SimpleSchemaRepository repository = getRepository();
    XmlSchemaConfig xml = new XmlSchemaConfig();
    xml.setUniqueId(UUID.randomUUID());
    repository.addSchema("/root", xml);
    Assertions.assertNotNull(repository.getSchema(xml.getUniqueId()));
    Assertions.assertEquals(xml, repository.getSchema(xml.getUniqueId()));

    repository.removeSchema(xml.getUniqueId());
    Assertions.assertEquals(0, repository.getSchemaByContext("/root").size());
    Assertions.assertNull(repository.getSchema(xml.getUniqueId()));
    repository.removeAllSchemas();
    Assertions.assertEquals(0, repository.getAll().size());

  }

  @Test
  void simpleAddRemoveAllAccess() throws IOException {
    SimpleSchemaRepository repository = getRepository();
    XmlSchemaConfig xml = new XmlSchemaConfig();
    xml.setUniqueId(UUID.randomUUID());
    repository.addSchema("/root", xml);
    Assertions.assertNotNull(repository.getSchema(xml.getUniqueId()));
    Assertions.assertEquals(xml, repository.getSchema(xml.getUniqueId()));

    repository.removeAllSchemas();
    Assertions.assertEquals(0, repository.getSchemaByContext("/root").size());
    Assertions.assertNull(repository.getSchema(xml.getUniqueId()));
    repository.removeAllSchemas();
    Assertions.assertEquals(0, repository.getAll().size());

  }

}
