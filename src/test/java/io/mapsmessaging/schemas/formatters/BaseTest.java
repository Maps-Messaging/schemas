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

import com.github.javafaker.Faker;
import io.mapsmessaging.schemas.config.SchemaConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class BaseTest {

  List<Person> createList(int size){
    Faker faker = new Faker();
    List<Person> list = new ArrayList<>();
    for(int x=0;x<size;x++){
      Person p = new Person();
      p.setEmail(faker.internet().emailAddress());
      switch(x%6){
        case 0:
          p.setName(faker.lordOfTheRings().character());
          break;
        case 1:
          p.setName(faker.gameOfThrones().character());
          break;
        case 2:
          p.setName(faker.howIMetYourMother().character());
          break;
        case 3:
          p.setName(faker.hitchhikersGuideToTheGalaxy().character());
          break;
        case 4:
          p.setName(faker.harryPotter().character());
          break;
        case 5:
          p.setName(faker.backToTheFuture().character());
          break;

      }
      p.setId(x);
      list.add(p);
    }
    return list;
  }

  abstract List<byte[]> packList(List<Person>  list) throws IOException;

  abstract SchemaConfig getSchema() throws IOException;

  @Test
  void testFormatters() throws IOException {
    List<Person> list = createList(1_000);
    List<byte[]> packed = packList(list);
    SchemaConfig schemaConfig = getSchema();
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(schemaConfig);
    for(int x=0;x<list.size();x++){
      ParsedObject parsedObject = formatter.parse(packed.get(x));
      Person p = list.get(x);
      Assertions.assertEquals(p.getName(), parsedObject.get("name"));
      Assertions.assertEquals(p.getId(),  parsedObject.get("id"));
      Assertions.assertEquals(p.getEmail(), parsedObject.get("email"));
    }
  }
}
