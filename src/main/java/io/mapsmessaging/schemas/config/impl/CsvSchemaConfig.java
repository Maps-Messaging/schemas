/*
 *
 *     Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

package io.mapsmessaging.schemas.config.impl;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.CSV_HEADER_NOT_DEFINED;

import io.mapsmessaging.schemas.config.SchemaConfig;
import java.io.IOException;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

/**
 * The type Csv schema config.
 */
public class CsvSchemaConfig extends SchemaConfig {

  private static final String NAME = "CSV";
  private static final String HEADER = "header";
  private static final String NUMERIC_STRINGS = "numericStrings";

  @Getter
  @Setter
  private String headerValues;

  @Getter
  @Setter
  private boolean interpretNumericStrings;


  /**
   * Instantiates a new Csv schema config.
   */
  public CsvSchemaConfig() {
    super(NAME);
    setMimeType("text/plain");
  }

  /**
   * Instantiates a new Csv schema config.
   *
   * @param header the header
   * @param interpretNumericStrings the interpret numeric strings
   */
  public CsvSchemaConfig(String header, boolean interpretNumericStrings) {
    super(NAME);
    this.headerValues = header;
    this.interpretNumericStrings = interpretNumericStrings;
    setMimeType("text/plain");
  }

  /**
   * Instantiates a new Csv schema config.
   *
   * @param config the config
   */
  protected CsvSchemaConfig(Map<String, Object> config) {
    super(NAME, config);
    this.headerValues = config.getOrDefault(HEADER, "").toString();
    this.interpretNumericStrings = Boolean.parseBoolean(config.getOrDefault(NUMERIC_STRINGS, "false").toString());
  }


  @Override
  protected JSONObject packData() throws IOException {
    if (headerValues == null || headerValues.length() == 0) {
      logger.log(CSV_HEADER_NOT_DEFINED, format, uniqueId);
      throw new IOException("No header specified");
    }
    JSONObject data = new JSONObject();
    packData(data);
    data.put(HEADER, headerValues);
    data.put(NUMERIC_STRINGS, interpretNumericStrings);
    return data;
  }

  protected SchemaConfig getInstance(Map<String, Object> config) {
    return new CsvSchemaConfig(config);
  }
}
