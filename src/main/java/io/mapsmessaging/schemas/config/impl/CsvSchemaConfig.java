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

package io.mapsmessaging.schemas.config.impl;

import com.google.gson.JsonObject;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.Map;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.CSV_HEADER_NOT_DEFINED;

/**
 * The type Csv schema config.
 */
@Schema(description = "CVS Schema Configuration")
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
  protected JsonObject packData() throws IOException {
    if (headerValues == null || headerValues.isEmpty()) {
      logger.log(CSV_HEADER_NOT_DEFINED, format, uniqueId);
      throw new IOException("No header specified");
    }
    JsonObject data = new JsonObject();
    packData(data);
    data.addProperty(HEADER, headerValues);
    data.addProperty(NUMERIC_STRINGS, interpretNumericStrings);
    return data;
  }

  protected SchemaConfig getInstance(Map<String, Object> config) {
    return new CsvSchemaConfig(config);
  }
}
