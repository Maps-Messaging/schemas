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
package io.mapsmessaging.schemas.config;

import java.io.IOException;
import java.util.Map;
import lombok.Getter;
import org.json.JSONObject;

public abstract class SchemaConfig {

  @Getter
  private final String format;

  public SchemaConfig(String format) {
    this.format = format;
  }

  public String pack() throws IOException {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("schema", packData());
    return jsonObject.toString(2);
  }

  protected void packData(JSONObject jsonObject) {
    jsonObject.put("format", format);
  }

  protected abstract JSONObject packData() throws IOException;

  protected abstract SchemaConfig getInstance(Map<String, Object> config);

}
