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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public abstract class BaseTest {

  private static List<Person> data;

  static List<Person> createList(int size){
    Faker faker = new Faker();
    List<Person> list = new ArrayList<>();
    for(int x=0;x<size;x++){
      Person p = new Person();
      switch(x%6){
        case 0:
          p.setStringId(faker.lordOfTheRings().character());
          break;
        case 1:
          p.setStringId(faker.gameOfThrones().character());
          break;
        case 2:
          p.setStringId(faker.howIMetYourMother().character());
          break;
        case 3:
          p.setStringId(faker.hitchhikersGuideToTheGalaxy().character());
          break;
        case 4:
          p.setStringId(faker.harryPotter().character());
          break;
        case 5:
          p.setStringId(faker.backToTheFuture().character());
          break;

      }
      p.setLongId(faker.random().nextLong());
      p.setIntId((int)(faker.random().nextLong()));
      p.setDoubleId(faker.random().nextDouble());
      p.setFloatId((float)faker.random().nextDouble());
      list.add(p);
    }
    return list;
  }

  @BeforeAll
  static void createData(){
    data = createList(1_000_000);
  }

  abstract List<byte[]> packList(List<Person>  list) throws IOException;

  abstract SchemaConfig getSchema() throws IOException;

  @Test
  void testFormatters() throws IOException {
    long start = System.currentTimeMillis();
    List<byte[]> packed = packList(data);
    System.err.println("Time to Pack:"+(System.currentTimeMillis() - start)+"ms");
    start = System.currentTimeMillis();
    SchemaConfig schemaConfig = getSchema();
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(schemaConfig);
    for(int x=0;x<data.size();x++){
      ParsedObject parsedObject = formatter.parse(packed.get(x));
      Person p = data.get(x);
      validateValues(p.getStringId(), parsedObject.get("stringId"));
      validateValues(p.getLongId(), parsedObject.get("longId"));
      validateValues(p.getIntId(), parsedObject.get("intId"));
      validateValues(p.getFloatId(), parsedObject.get("floatId"));
      validateValues(p.getDoubleId(), parsedObject.get("doubleId"));
    }
    System.err.println("Time to Parse:"+(System.currentTimeMillis() - start)+"ms");

  }

  private void validateValues(Object lhs, Object rhs){
    if(lhs instanceof Float && rhs instanceof Double) {
      rhs = ((Double) rhs).floatValue();
    }
    BigDecimal vlhs = convert(lhs);
    BigDecimal vrhs = convert(rhs);

    Assertions.assertEquals(vlhs, vrhs);
  }

  BigDecimal convert(Object obj){
    if(obj instanceof Long){
      return new BigDecimal((Long)obj);
    }
    if(obj instanceof Double){
      return BigDecimal.valueOf((Double) obj);
    }
    if(obj instanceof Integer){
      return new BigDecimal((Integer)obj);
    }
    if(obj instanceof Float){
      return BigDecimal.valueOf((Float) obj);
    }
    return null;
  }
}
