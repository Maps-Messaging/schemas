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
import java.util.Base64;

/**
 * The type Proto buf schema config.
 */
@Schema(description = "Protobuf Schema Configuration")
public class ProtoBufSchemaConfig extends SchemaConfig {

  private static final String DESCRIPTOR = "descriptor";
  private static final String MESSAGE_NAME = "messageName";


  /**
   * Instantiates a new Proto buf schema config.
   */
  public ProtoBufSchemaConfig() {
    super("protobuf");
  }

  /**
   * Instantiates a new Proto buf schema config.
   *
   * @param config the config
   */
  protected ProtoBufSchemaConfig(SchemaConfig config) {
    super(config);
  }

  public String getMimeType() {
    return "application/octet-stream";
  }

  public ProtobufConfig getProtobufConfig() {
    JsonObject obj = getSchema();
    ProtobufConfig protobufConfig = new ProtobufConfig();
    if (obj != null) {
      if (obj.has(MESSAGE_NAME)) {
        protobufConfig.setMessageName(obj.get(MESSAGE_NAME).getAsString());
      }
      if (obj.has(DESCRIPTOR)) {
        protobufConfig.descriptorValue = Base64.getDecoder().decode(obj.get(DESCRIPTOR).getAsString());
      }
    }
    return protobufConfig;
  }

  public void setProtobufConfig(ProtobufConfig protobufConfig) {
    JsonObject obj = new JsonObject();
    if (protobufConfig.messageName != null) {
      obj.addProperty(MESSAGE_NAME, protobufConfig.messageName);
    }
    if (protobufConfig.descriptorValue != null) {
      obj.addProperty(DESCRIPTOR, Base64.getEncoder().encodeToString(protobufConfig.descriptorValue));
    }
    setSchema(obj);
  }

  public SchemaConfig getInstance(SchemaConfig config) {
    return new ProtoBufSchemaConfig(config);
  }

  @Override
  public String pack() throws IOException {
    ProtobufConfig cfg = getProtobufConfig();
    if (cfg != null) {
      if (cfg.getDescriptorValue() == null || cfg.getDescriptorValue().length == 0) {
        throw new IOException("Protobuf Descriptor Value is null or empty");
      }
      if (cfg.getMessageName() == null || cfg.getMessageName().isEmpty()) {
        throw new IOException("Protobuf Message Name is null or empty");
      }
    }
    return super.pack();
  }

  @Getter
  @Setter
  public static final class ProtobufConfig {
    private String messageName;
    private byte[] descriptorValue;
  }

}