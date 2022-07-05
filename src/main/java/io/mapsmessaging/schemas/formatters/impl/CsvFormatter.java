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

package io.mapsmessaging.schemas.formatters.impl;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.FORMATTER_UNEXPECTED_OBJECT;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.CsvSchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.walker.MapResolver;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.json.JSONObject;

public class CsvFormatter extends MessageFormatter {

  private final String[] keys;
  private final boolean interpretNumericStrings;
  private final CsvParser parser;

  public CsvFormatter() {
    keys = new String[0];
    parser = null;
    interpretNumericStrings = false;
  }

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
    String[] values = parser.parseLine(new String(payload));
    Map<String, Object> map = new LinkedHashMap<>();
    for (int x = 0; x < (Math.min(values.length, keys.length)); x++) {
      map.put(keys[x], values[x]);
    }
    return new MapResolver(map, interpretNumericStrings);
  }

  @Override
  public JSONObject parseToJson(byte[] payload) throws IOException {
    Map<String, Object> map = (Map) parse(payload);
    return new JSONObject(map);
  }

  @Override
  public byte[] pack(Object object) throws IOException {
    String toPack = null;
    if (object instanceof String) {
      toPack = (String) object;
    }
    if (object instanceof JSONObject) {
      JSONObject jsonObject = (JSONObject) object;
      toPack = packMap(jsonObject.toMap());
    }
    if (object instanceof Map) {
      toPack = packMap((Map) object);
    }

    if (toPack != null) {
      return toPack.getBytes(StandardCharsets.UTF_8);
    }
    logger.log(FORMATTER_UNEXPECTED_OBJECT, getName(), object.getClass().toString());
    throw new IOException("Unexpected object to be packed");
  }

  private String packMap(Map<String, Object> map) {
    StringBuilder stringBuilder = new StringBuilder();
    boolean first = true;
    for (String key : keys) {
      if (!first) {
        stringBuilder.append(",");
      }
      first = false;
      Object val = map.get(key);
      if (val != null) {
        stringBuilder.append(val);
      }
    }
    return stringBuilder.toString();
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

}
