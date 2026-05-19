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
import io.mapsmessaging.schemas.tools.protobuf.ProtobufBundleMessageExtractor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.*;

/**
 * The type Proto buf schema config.
 */
@Schema(description = "Protobuf Schema Configuration")
public class ProtoBufSchemaConfig extends SchemaConfig {

  private static final String DESCRIPTOR = "descriptor";
  private static final String MESSAGE_NAME = "messageName";

  private List<SchemaConfig> bundledSchemas = null;
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

  @Override
  public String getMimeType() {
    return "application/octet-stream";
  }

  public ProtobufConfig getProtobufConfig() {
    JsonObject obj = getSchema();
    ProtobufConfig protobufConfig = new ProtobufConfig();
    if (obj != null) {
      if (obj.has(MESSAGE_NAME) && !obj.get(MESSAGE_NAME).isJsonNull()) {
        protobufConfig.setMessageName(obj.get(MESSAGE_NAME).getAsString());
      }
      if (obj.has(DESCRIPTOR) && !obj.get(DESCRIPTOR).isJsonNull()) {
        protobufConfig.setDescriptorValue(Base64.getDecoder().decode(obj.get(DESCRIPTOR).getAsString()));
      }
    }
    return protobufConfig;
  }

  public void setProtobufConfig(ProtobufConfig protobufConfig) {
    JsonObject obj = new JsonObject();
    if (protobufConfig != null) {
      if (protobufConfig.getMessageName() != null && !protobufConfig.getMessageName().isEmpty()) {
        obj.addProperty(MESSAGE_NAME, protobufConfig.getMessageName());
      }
      if (protobufConfig.getDescriptorValue() != null && protobufConfig.getDescriptorValue().length > 0) {
        obj.addProperty(DESCRIPTOR, Base64.getEncoder().encodeToString(protobufConfig.getDescriptorValue()));
      }
    }
    setSchema(obj);
  }

  @Override
  public SchemaConfig getInstance(SchemaConfig config) {
    return new ProtoBufSchemaConfig(config);
  }

  @Override
  public boolean isBundle() {
    ProtobufConfig config = getProtobufConfig();
    return config.getDescriptorValue() != null &&
        config.getDescriptorValue().length > 0 &&
        (config.getMessageName() == null ||
            config.getMessageName().isEmpty() ||
            config.getMessageName().equals("all")
        );
  }

  @Override
  public synchronized List<SchemaConfig> getBundledSchemas() {
    if (bundledSchemas == null) {
      bundledSchemas = new ArrayList<>();
      try {
        List<String> messageNames = ProtobufBundleMessageExtractor.extractMessageNames(getProtobufConfig().descriptorValue);
        for (String messageName : messageNames) {
          String name = (getUniqueId() + "::" + messageName);
          ProtobufConfig config = new ProtobufConfig();
          config.setDescriptorValue(null);
          config.setMessageName(messageName);

          ProtoBufSchemaConfig schema = new ProtoBufSchemaConfig();
          schema.setUniqueId(UUID.nameUUIDFromBytes(name.getBytes()));
          schema.setParentUuid(getUniqueId());
          schema.setName(name);
          schema.setSource(getSource());
          schema.setVersion(getVersion());
          schema.setProtobufConfig(config);

          bundledSchemas.add(schema);
        }
      } catch (IOException e) {
        // log this
      }
      bundledSchemas = Collections.unmodifiableList(bundledSchemas);
    }
    return bundledSchemas;
  }

  @Override
  public String pack() throws IOException {
    ProtobufConfig config = getProtobufConfig();
    if (config == null) {
      throw new IOException("Protobuf configuration is null");
    }

    if (config.getDescriptorValue() == null || config.getDescriptorValue().length == 0) {
      throw new IOException("Protobuf Descriptor Value is null or empty");
    }

    if (!isBundle() && (config.getMessageName() == null || config.getMessageName().isEmpty())) {
      throw new IOException("Protobuf Message Name is null or empty");
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