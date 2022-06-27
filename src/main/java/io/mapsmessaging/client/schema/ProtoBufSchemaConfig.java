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
package io.mapsmessaging.client.schema;

import java.util.Base64;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

public class ProtoBufSchemaConfig extends SchemaConfig {

  @Getter
  @Setter
  private byte[] descriptor;

  @Getter
  @Setter
  private String messageName;

  public ProtoBufSchemaConfig() {
    super("ProtoBuf");
  }

  protected ProtoBufSchemaConfig(String name, String base64EncodedDescriptor) {
    super("ProtoBuf");
    messageName = name;
    descriptor = Base64.getDecoder().decode(base64EncodedDescriptor);
  }


  @Override
  protected JSONObject packData() {
    JSONObject data = new JSONObject();
    packData(data);
    data.put("descriptor", new String(Base64.getEncoder().encode(descriptor)));
    data.put("messageName", messageName);
    return data;
  }

  protected SchemaConfig getInstance(JSONObject config) {
    return new ProtoBufSchemaConfig(config.getString("messageName"), config.getString("descriptor"));
  }

}