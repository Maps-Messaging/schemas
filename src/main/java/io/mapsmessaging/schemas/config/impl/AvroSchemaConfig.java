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


/**
 * The type Avro schema config.
 */
@Schema(description = "AVRO Schema Configuration")
public class AvroSchemaConfig extends SchemaConfig {

  public AvroSchemaConfig() {
    super("avro");
  }

  protected AvroSchemaConfig(SchemaConfig config) {
    super(config);
  }

  public String getAvroSchema() {
    JsonObject json = getSchema();
    return json.get("schema").getAsString();
  }

  public void setAvroSchema(String avroSchema) {
    JsonObject json = new JsonObject();
    json.addProperty("schema", avroSchema);
    setSchema(json);
  }

  @Override
  public String getMimeType() {
    return "application/octet-stream";
  }

  @Override
  public SchemaConfig getInstance(SchemaConfig config) {
    return new AvroSchemaConfig(config);
  }
}
