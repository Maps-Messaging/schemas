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

public class XmlSchemaConfig extends SimpleSchemaConfig {

  @Getter
  @Setter
  private boolean namespaceAware = false;

  @Getter
  @Setter
  private boolean validating = false;

  @Getter
  @Setter
  private boolean coalescing = false;

  public XmlSchemaConfig() {
    super("XML");
  }

  protected SchemaConfig getInstance(Map<String, Object> config) {
    return this;
  }

  @Override
  protected JSONObject packData() {
    JSONObject data = new JSONObject();
    packData(data);
    data.put("namespaceAware", namespaceAware);
    data.put("validating", validating);
    data.put("coalescing", coalescing);
    return data;
  }

}
