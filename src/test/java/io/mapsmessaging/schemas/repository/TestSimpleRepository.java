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

package io.mapsmessaging.schemas.repository;

import io.mapsmessaging.schemas.config.impl.XmlSchemaConfig;
import io.mapsmessaging.schemas.repository.impl.SimpleSchemaRepository;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestSimpleRepository {

  @Test
  void simpleAccess(){
    SimpleSchemaRepository repository = new SimpleSchemaRepository();
    XmlSchemaConfig xml = new XmlSchemaConfig();
    xml.setUniqueId(UUID.randomUUID());
    repository.addSchema("/root", xml);
    Assertions.assertNotNull(repository.getSchema(xml.getUniqueId()));
    Assertions.assertEquals(xml, repository.getSchema(xml.getUniqueId()));

    Assertions.assertNotNull(repository.getSchema("/root"));
    Assertions.assertEquals(xml, repository.getSchema("/root").get(0));

    repository.removeAllSchemas();
    Assertions.assertNull(repository.getSchema("/root"));
    Assertions.assertNull(repository.getSchema(xml.getUniqueId()));
  }
}
