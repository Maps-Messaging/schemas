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
import java.util.Base64;
import java.util.Map;
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
  protected JSONObject packData() throws IOException {
    if(descriptor == null || descriptor.length == 0){
      throw new IOException("No descriptor specified");
    }
    if(messageName == null || messageName.length() == 0){
      throw new IOException("No message name specified");
    }

    JSONObject data = new JSONObject();
    packData(data);
    data.put("descriptor", new String(Base64.getEncoder().encode(descriptor)));
    data.put("messageName", messageName);
    return data;
  }

  protected SchemaConfig getInstance(Map<String, Object> config) {
    return new ProtoBufSchemaConfig(config.get("messageName").toString(), config.get("descriptor").toString());
  }

}