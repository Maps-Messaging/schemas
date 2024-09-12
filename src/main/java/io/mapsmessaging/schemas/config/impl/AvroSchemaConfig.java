/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package io.mapsmessaging.schemas.config.impl;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import static io.mapsmessaging.schemas.config.Constants.SCHEMA;
import static io.mapsmessaging.schemas.logging.SchemaLogMessages.AVRO_SCHEMA_NOT_DEFINED;

/**
 * The type Avro schema config.
 */
@Schema(description = "AVRO Schema Configuration")
public class AvroSchemaConfig extends SchemaConfig {

  private static final String NAME = "AVRO";

  @Getter
  @Setter
  private String schema;

  /**
   * Instantiates a new Avro schema config.
   */
  public AvroSchemaConfig() {
    super(NAME);
    setMimeType("application/octet-stream");
  }

  /**
   * Instantiates a new Avro schema config.
   *
   * @param config the config
   */
  protected AvroSchemaConfig(Map<String, Object> config) {
    super(NAME, config);
    this.schema = new String(Base64.getDecoder().decode(config.get(SCHEMA).toString()));
    setMimeType("application/octet-stream");
  }


  @Override
  protected JSONObject packData() throws IOException {
    if (schema == null || schema.length() == 0) {
      logger.log(AVRO_SCHEMA_NOT_DEFINED, format, uniqueId);
      throw new IOException("No schema specified");
    }
    JSONObject data = new JSONObject();
    packData(data);
    data.put(SCHEMA, new String(Base64.getEncoder().encode(schema.getBytes())));
    return data;
  }

  protected SchemaConfig getInstance(Map<String, Object> config) {
    return new AvroSchemaConfig(config);
  }
}
