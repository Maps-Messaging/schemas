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

package io.mapsmessaging.schemas.formatters;

import com.github.javafaker.Faker;
import com.google.gson.JsonObject;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseTest {

  private static final String[] UNIT = {"ms", "μs", "ns"};
  private static List<Person> data;

  static List<Person> createList() {
    Faker faker = new Faker();
    List<Person> list = new ArrayList<>();
    for (int x = 0; x < 1000; x++) {
      Person p = new Person();
      switch (x % 6) {
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

        default:
          break;

      }
      p.setLongId(faker.random().nextLong());
      p.setIntId((int) (faker.random().nextLong()));
      p.setDoubleId(faker.random().nextDouble());
      p.setFloatId((float) faker.random().nextDouble());
      list.add(p);
    }
    return list;
  }

  @BeforeAll
  static void createData() {
    data = createList();
  }

  abstract List<byte[]> packList(List<Person> list) throws IOException;

  abstract SchemaConfig getSchema() throws IOException;

  @Test
  void testInvalidData() throws IOException {
    SchemaConfig schemaConfig = getSchema();
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(schemaConfig);
    Assertions.assertNotNull(formatter.parse("This should not be parsable".getBytes()));
    Assertions.assertNull(formatter.parse("This should not be parsable".getBytes()).get("value"));
    byte[] binary = new byte[1024];
    for (int x = 0; x < binary.length; x++) {
      binary[x] = (byte) (x % 0xf);
    }
    Assertions.assertNotNull(formatter.parse(binary));
    Assertions.assertNull(formatter.parse(binary).get("value"));
  }

  @Test
  void testFormatterToJSON() throws IOException {
    List<byte[]> packed = packList(data);
    SchemaConfig schemaConfig = getSchema();
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(schemaConfig);
    for (int x = 0; x < data.size(); x++) {
      Person p = data.get(x);
      JsonObject jsonObject = formatter.parseToJson(packed.get(x));
      validateValues(p.getStringId(), jsonObject.get("stringId").getAsString());
      validateValues(p.getLongId(), jsonObject.get("longId").getAsLong());
      validateValues(p.getIntId(), jsonObject.get("intId").getAsInt());
      validateValues(p.getFloatId(), jsonObject.get("floatId").getAsFloat());
      validateValues(p.getDoubleId(), jsonObject.get("doubleId").getAsDouble());
    }
  }


  @Test
  void testFormatters() throws IOException {
    long start = System.currentTimeMillis();
    List<byte[]> packed = packList(data);
    System.err.println("Time to Pack:" + (System.currentTimeMillis() - start) + "ms");
    start = System.currentTimeMillis();
    SchemaConfig schemaConfig = getSchema();
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(schemaConfig);
    for (int x = 0; x < data.size(); x++) {
      ParsedObject parsedObject = formatter.parse(packed.get(x));
      Person p = data.get(x);
      validateValues(p.getStringId(), parsedObject.get("stringId"));
      validateValues(p.getLongId(), parsedObject.get("longId"));
      validateValues(p.getIntId(), parsedObject.get("intId"));
      validateValues(p.getFloatId(), parsedObject.get("floatId"));
      validateValues(p.getDoubleId(), parsedObject.get("doubleId"));
    }
    long time = (System.currentTimeMillis() - start);
    float unitWork = time;
    unitWork = unitWork / data.size();
    System.err.println("Time to Parse:" + time + "ms");
    int scale = 0;
    while ((int) unitWork == 0) {
      unitWork = unitWork * 1000f;
      scale++;
    }
    System.err.println("Time per event " + unitWork + UNIT[scale]);
  }

  @Test
  void testParallelFormatters() throws IOException {
    long start = System.currentTimeMillis();
    List<byte[]> packed = packList(data);
    System.err.println("Time to Pack:" + (System.currentTimeMillis() - start) + "ms");
    List<DataSet> dataSet = new ArrayList<>();
    for (int x = 0; x < data.size(); x++) {
      dataSet.add(new DataSet(data.get(x), packed.get(x)));
    }
    start = System.currentTimeMillis();
    SchemaConfig schemaConfig = getSchema();
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(schemaConfig);
    dataSet.parallelStream().forEach(set -> {
      ParsedObject parsedObject = formatter.parse(set.packed);
      Person p = set.source;
      validateValues(p.getStringId(), parsedObject.get("stringId"));
      validateValues(p.getLongId(), parsedObject.get("longId"));
      validateValues(p.getIntId(), parsedObject.get("intId"));
      validateValues(p.getFloatId(), parsedObject.get("floatId"));
      validateValues(p.getDoubleId(), parsedObject.get("doubleId"));
    });
    long time = (System.currentTimeMillis() - start);
    float unitWork = time;
    unitWork = unitWork / data.size();
    System.err.println("Time to Parse:" + time + "ms");
    int scale = 0;
    while ((int) unitWork == 0) {
      unitWork = unitWork * 1000f;
      scale++;
    }
    System.err.println("Time per event " + unitWork + UNIT[scale]);
  }

  @Test
  void testFiltering() throws IOException, ParseException {
    Faker faker = new Faker();
    long start = System.currentTimeMillis();
    List<byte[]> packed = packList(data);
    System.err.println("Time to Pack:" + (System.currentTimeMillis() - start) + "ms");
    List<DataSet> dataSet = new ArrayList<>();
    for (int x = 0; x < data.size(); x++) {
      dataSet.add(new DataSet(data.get(x), packed.get(x)));
    }
    start = System.currentTimeMillis();
    SchemaConfig schemaConfig = getSchema();
    String selector = "stringId = '" + data.get(faker.random().nextInt(0, data.size() -1)).getStringId() + "' OR " +
        "longId = " + data.get(faker.random().nextInt(0, data.size() -1)).getLongId() + " OR " +
        "intId = " + data.get(faker.random().nextInt(0, data.size() -1)).getIntId() + " OR " +
        "doubleId = " + data.get(faker.random().nextInt(0, data.size() -1)).getDoubleId() + " OR " +
        "floatId = " + data.get(faker.random().nextInt(0, data.size() -1)).getFloatId();

    ParserExecutor executor = SelectorParser.compile(selector);
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(schemaConfig);
    List<DataSet> result = dataSet.parallelStream().filter(dataSet1 -> executor.evaluate(formatter.parse(dataSet1.packed))).collect(Collectors.toList());

    Assertions.assertTrue(result.size() >= 5);
    long time = (System.currentTimeMillis() - start);
    float unitWork = time;
    unitWork = unitWork / data.size();
    System.err.println("Time to Filter:" + time + "ms");
    int scale = 0;
    while ((int) unitWork == 0) {
      unitWork = unitWork * 1000f;
      scale++;
    }
    System.err.println("Time per event " + unitWork + UNIT[scale]);
  }

  @Test
  void testFilteringLookup() throws IOException, ParseException {
    List<byte[]> packed = packList(data);
    List<DataSet> dataSet = new ArrayList<>();
    for (int x = 0; x < data.size(); x++) {
      dataSet.add(new DataSet(data.get(x), packed.get(x)));
    }
    SchemaConfig schemaConfig = getSchema();
    Faker faker = new Faker();
    int index = faker.random().nextInt(0, data.size() -1);

    ParserExecutor stringExecutor = SelectorParser.compile("stringId = '" + data.get(index).getStringId() + "'");
    ParserExecutor longExecutor = SelectorParser.compile("longId = " + data.get(index).getLongId());
    ParserExecutor intExecutor = SelectorParser.compile("intId = " + data.get(index).getIntId());
    ParserExecutor doubleExecutor = SelectorParser.compile("doubleId = " + data.get(index).getDoubleId());
    ParserExecutor floatExecutor = SelectorParser.compile("floatId = " + data.get(index).getFloatId());

    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(schemaConfig);
    Assertions.assertTrue(stringExecutor.evaluate(formatter.parse(dataSet.get(index).packed)));
    Assertions.assertTrue(longExecutor.evaluate(formatter.parse(dataSet.get(index).packed)));
    Assertions.assertTrue(intExecutor.evaluate(formatter.parse(dataSet.get(index).packed)));
    Assertions.assertTrue(doubleExecutor.evaluate(formatter.parse(dataSet.get(index).packed)));
    Assertions.assertTrue(floatExecutor.evaluate(formatter.parse(dataSet.get(index).packed)));
  }

  private void validateValues(Object lhs, Object rhs) {
    if (lhs instanceof String) {
      Assertions.assertEquals(lhs.toString(), rhs.toString());
    } else {
      if (lhs instanceof Float && rhs instanceof Double) {
        rhs = ((Double) rhs).floatValue();
      }
      BigDecimal vlhs = convert(lhs);
      BigDecimal vrhs = convert(rhs);
      double dlhs = vlhs.doubleValue();
      double drhs = vrhs.doubleValue();
      Assertions.assertEquals(dlhs, drhs, 1e-6);
    }
  }

  BigDecimal convert(Object obj) {
    if (obj instanceof Long) {
      return new BigDecimal((Long) obj);
    }
    if (obj instanceof Double) {
      return BigDecimal.valueOf((Double) obj);
    }
    if (obj instanceof Integer) {
      return new BigDecimal((Integer) obj);
    }
    if (obj instanceof Float) {
      return new BigDecimal(obj.toString());
    }
    if (obj instanceof BigDecimal) {
      return (BigDecimal) obj;
    }
    return null;
  }

  private static class DataSet {

    Person source;
    byte[] packed;

    public DataSet(Person p, byte[] r) {
      source = p;
      packed = r;
    }
  }
}
