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

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.model.XRegistrySchemaVersion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * The type Csv schema config.
 */
@Schema(description = "CVS Schema Configuration")
public class CsvSchemaConfig extends SchemaConfig {

  @Getter
  private CsvConfig config;

  /**
   * Instantiates a new Csv schema config.
   */
  public CsvSchemaConfig() {
    super("csv");
  }

  /**
   * Instantiates a new Csv schema config.
   *
   * @param header                  the header
   * @param interpretNumericStrings the interpret numeric strings
   */
  public CsvSchemaConfig(String header, boolean interpretNumericStrings) {
    super("CSV");
    config = new CsvConfig();
    config.setHeaderValues(header);
    config.setInterpretNumericStrings(interpretNumericStrings);
  }

  /**
   * Instantiates a new Csv schema config.
   *
   * @param config the config
   */
  protected CsvSchemaConfig(XRegistrySchemaVersion config) {
    super(config);
    if (config.getSchema() != null) {
      this.config = gson.fromJson(config.getSchema(), CsvConfig.class);
    }
  }

  @Override
  public String getMimeType() {
    return "text/csv";
  }

  @Override
  public SchemaConfig getInstance(XRegistrySchemaVersion config) {
    return new CsvSchemaConfig(config);
  }

  public void setConfig(CsvConfig csvConfig) {
    this.config = csvConfig;
    setSchema(gson.toJsonTree(csvConfig).getAsJsonObject());
  }

  @Getter
  @Setter
  public static final class CsvConfig {
    private String headerValues;
    private boolean interpretNumericStrings;
  }
}
