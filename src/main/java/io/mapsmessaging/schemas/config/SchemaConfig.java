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

import static io.mapsmessaging.schemas.config.Constants.EXPIRES_AFTER;
import static io.mapsmessaging.schemas.config.Constants.FORMAT;
import static io.mapsmessaging.schemas.config.Constants.NOT_BEFORE;
import static io.mapsmessaging.schemas.config.Constants.SCHEMA;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

public abstract class SchemaConfig {

  protected Logger logger;

  @Getter
  protected final String format;

  @Getter
  @Setter
  protected UUID uniqueId;

  @Getter
  @Setter
  private LocalDateTime expiresAfter;

  @Getter
  @Setter
  private LocalDateTime notBefore;

  protected SchemaConfig(String format) {
    this.format = format;
    logger = LoggerFactory.getLogger(SchemaConfig.class);
  }

  protected SchemaConfig(String format, Map<String, Object> config){
    this(format);
    uniqueId = UUID.fromString(config.get(io.mapsmessaging.schemas.config.Constants.UUID).toString());
    if(config.containsKey(EXPIRES_AFTER)){
      expiresAfter = LocalDateTime.parse(config.get(EXPIRES_AFTER).toString());
    }
    if(config.containsKey(NOT_BEFORE)){
      notBefore = LocalDateTime.parse(config.get(NOT_BEFORE).toString());
    }
  }

  public String pack() throws IOException {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(SCHEMA, packData());
    return jsonObject.toString(2);
  }

  protected void packData(JSONObject jsonObject) {
    jsonObject.put(FORMAT, format);
    if(expiresAfter != null)jsonObject.put(EXPIRES_AFTER, expiresAfter.toString() );
    if(notBefore != null)jsonObject.put(NOT_BEFORE, notBefore.toString() );
    if(notBefore != null)jsonObject.put(NOT_BEFORE, notBefore.toString() );
    jsonObject.put(io.mapsmessaging.schemas.config.Constants.UUID, uniqueId.toString());

  }

  protected abstract JSONObject packData() throws IOException;

  protected abstract SchemaConfig getInstance(Map<String, Object> config);

}
