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

package io.mapsmessaging.schemas.formatters.impl;

import com.google.gson.JsonObject;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.CsvSchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.walker.MapResolver;

import java.io.IOException;
import java.util.*;

/**
 * The type Csv formatter.
 */
public class CsvFormatter extends MessageFormatter {

  private final String[] keys;
  private final boolean interpretNumericStrings;
  private final CsvParser parser;

  /**
   * Instantiates a new Csv formatter.
   */
  public CsvFormatter() {
    keys = new String[0];
    parser = null;
    interpretNumericStrings = false;
  }

  /**
   * Instantiates a new Csv formatter.
   *
   * @param keyList the key list
   * @param interpretNumericStrings the interpret numeric strings
   */
  public CsvFormatter(String keyList, boolean interpretNumericStrings) {
    StringTokenizer tokenizer = new StringTokenizer(keyList, ",");
    List<String> header = new ArrayList<>();
    while (tokenizer.hasMoreElements()) {
      header.add(tokenizer.nextElement().toString().trim());
    }
    String[] tmp = new String[header.size()];
    keys = header.toArray(tmp);
    this.interpretNumericStrings = interpretNumericStrings;
    CsvParserSettings settings = new CsvParserSettings();
    parser = new CsvParser(settings);

  }

  @Override
  public synchronized ParsedObject parse(byte[] payload) {
    return new MapResolver(parseToMap(payload), interpretNumericStrings);
  }

  @Override
  public JsonObject parseToJson(byte[] payload) throws IOException {
    ParsedObject parsedObject = parse(payload);
    JsonObject jsonObject = new JsonObject();
    for (String key : keys) {
      Object value = parsedObject.get(key);
      if (value != null) {
        jsonObject.add(key, gson.toJsonTree(value));
      }
    }
    return jsonObject;
  }

  @Override
  public MessageFormatter getInstance(SchemaConfig config) throws IOException {
    CsvSchemaConfig csvSchemaConfig = (CsvSchemaConfig) config;
    return new CsvFormatter(csvSchemaConfig.getHeaderValues(), csvSchemaConfig.isInterpretNumericStrings());
  }

  @Override
  public String getName() {
    return "CSV";
  }

  private Map<String, Object> parseToMap(byte[] payload) {
    String[] values = parser.parseLine(new String(payload));
    Map<String, Object> map = new LinkedHashMap<>();
    for (int x = 0; x < (Math.min(values.length, keys.length)); x++) {
      map.put(keys[x], values[x]);
    }
    return map;
  }

}
