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

package io.mapsmessaging.schemas.config.impl;

import io.mapsmessaging.schemas.config.SchemaConfig;
import java.io.IOException;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

public class CsvSchemaConfig extends SchemaConfig {

  private static final String NAME = "CSV";
  private static final String HEADER = "header";

  @Getter
  @Setter
  private String headerValues;

  public CsvSchemaConfig() {
    super(NAME);
  }

  public CsvSchemaConfig(String header) {
    super(NAME);
    this.headerValues = header;
  }

  protected CsvSchemaConfig(Map<String, Object> config) {
    super(NAME, config);
    this.headerValues = config.getOrDefault(HEADER, "").toString();
  }


  @Override
  protected JSONObject packData() throws IOException {
    if(headerValues == null || headerValues.length() == 0){
      throw new IOException("No header specified");
    }
    JSONObject data = new JSONObject();
    packData(data);
    data.put(HEADER, headerValues);
    return data;
  }

  protected SchemaConfig getInstance(Map<String, Object> config) {
    return new CsvSchemaConfig(config);
  }
}
