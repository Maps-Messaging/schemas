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

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.PROTOBUF_DESCRIPTOR_NOT_DEFINED;
import static io.mapsmessaging.schemas.logging.SchemaLogMessages.PROTOBUF_MESSAGE_NAME_NOT_DEFINED;

import io.mapsmessaging.schemas.config.SchemaConfig;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

/**
 * The type Proto buf schema config.
 */
public class ProtoBufSchemaConfig extends SchemaConfig {

  private static final String NAME = "ProtoBuf";
  private static final String DESCRIPTOR = "descriptor";
  private static final String MESSAGE_NAME = "messageName";

  @Getter
  @Setter
  private byte[] descriptorValue;

  @Getter
  @Setter
  private String messageName;

  /**
   * Instantiates a new Proto buf schema config.
   */
  public ProtoBufSchemaConfig() {
    super(NAME);
    setMimeType("application/octet-stream");
  }

  /**
   * Instantiates a new Proto buf schema config.
   *
   * @param config the config
   */
  protected ProtoBufSchemaConfig(Map<String, Object> config) {
    super(NAME, config);
    messageName = config.getOrDefault(MESSAGE_NAME, "").toString();
    descriptorValue = Base64.getDecoder().decode(config.getOrDefault(DESCRIPTOR, "").toString());
    setMimeType("application/octet-stream");
  }


  @Override
  protected JSONObject packData() throws IOException {
    if (descriptorValue == null || descriptorValue.length == 0) {
      logger.log(PROTOBUF_DESCRIPTOR_NOT_DEFINED, format, uniqueId);
      throw new IOException("No descriptor specified");
    }
    if (messageName == null || messageName.length() == 0) {
      logger.log(PROTOBUF_MESSAGE_NAME_NOT_DEFINED, format, uniqueId);
      throw new IOException("No message name specified");
    }

    JSONObject data = new JSONObject();
    packData(data);
    data.put(DESCRIPTOR, new String(Base64.getEncoder().encode(descriptorValue)));
    data.put(MESSAGE_NAME, messageName);
    return data;
  }

  protected SchemaConfig getInstance(Map<String, Object> config) {
    return new ProtoBufSchemaConfig(config);
  }

}