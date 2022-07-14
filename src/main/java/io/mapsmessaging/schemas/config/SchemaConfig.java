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

import static io.mapsmessaging.schemas.config.Constants.COMMENTS;
import static io.mapsmessaging.schemas.config.Constants.CREATION;
import static io.mapsmessaging.schemas.config.Constants.EXPIRES_AFTER;
import static io.mapsmessaging.schemas.config.Constants.FORMAT;
import static io.mapsmessaging.schemas.config.Constants.MIME_TYPE;
import static io.mapsmessaging.schemas.config.Constants.NOT_BEFORE;
import static io.mapsmessaging.schemas.config.Constants.SCHEMA;
import static io.mapsmessaging.schemas.config.Constants.SOURCE;

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

  @Getter
  protected final String format;
  protected Logger logger;

  @Getter
  protected String uniqueId;

  @Getter
  private LocalDateTime creation;

  @Getter
  @Setter
  private LocalDateTime expiresAfter;

  @Getter
  @Setter
  private LocalDateTime notBefore;

  @Setter
  @Getter
  private String comments;

  @Setter
  @Getter
  private String source;

  @Setter
  @Getter
  private String mimeType;

  protected SchemaConfig(String format) {
    this.format = format;
    logger = LoggerFactory.getLogger(SchemaConfig.class);
  }

  protected SchemaConfig(String format, Map<String, Object> config) {
    this(format);
    uniqueId = config.get(io.mapsmessaging.schemas.config.Constants.UUID).toString();
    if (config.containsKey(EXPIRES_AFTER)) {
      expiresAfter = LocalDateTime.parse(config.get(EXPIRES_AFTER).toString());
    }
    if (config.containsKey(NOT_BEFORE)) {
      notBefore = LocalDateTime.parse(config.get(NOT_BEFORE).toString());
    }
    if (config.containsKey(CREATION)) {
      creation = LocalDateTime.parse(config.get(CREATION).toString());
    }
    if (config.containsKey(COMMENTS)) {
      comments = config.get(COMMENTS).toString();
    }
    if (config.containsKey(SOURCE)) {
      source = config.get(SOURCE).toString();
    }
    if (config.containsKey(MIME_TYPE)) {
      mimeType = config.get(MIME_TYPE).toString();
    }
  }

  public String pack() throws IOException {
    return packtoJSON().toString(2);
  }

  public Map<String, Object> toMap() throws IOException {
    return packtoJSON().toMap();
  }

  public void setUniqueId(UUID uniqueId) {
    this.uniqueId = uniqueId.toString();
    creation = LocalDateTime.now();
  }

  public void setUniqueId(String uniqueId) {
    this.uniqueId = uniqueId;
    creation = LocalDateTime.now();
  }

  private JSONObject packtoJSON() throws IOException {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(SCHEMA, packData());
    return jsonObject;
  }

  protected void packData(JSONObject jsonObject) {
    jsonObject.put(FORMAT, format);
    if (expiresAfter != null) {
      jsonObject.put(EXPIRES_AFTER, expiresAfter.toString());
    }
    if (notBefore != null) {
      jsonObject.put(NOT_BEFORE, notBefore.toString());
    }
    if (notBefore != null) {
      jsonObject.put(NOT_BEFORE, notBefore.toString());
    }
    jsonObject.put(io.mapsmessaging.schemas.config.Constants.UUID, uniqueId);
    if (creation == null) {
      creation = LocalDateTime.now();
    }
    jsonObject.put(CREATION, creation.toString());

    if (comments != null && comments.length() > 0) {
      jsonObject.put(COMMENTS, comments);
    }
    if (source != null && source.length() > 0) {
      jsonObject.put(SOURCE, source);
    }
    if (mimeType != null && mimeType.length() > 0) {
      jsonObject.put(MIME_TYPE, mimeType);
    }

  }

  protected abstract JSONObject packData() throws IOException;

  protected abstract SchemaConfig getInstance(Map<String, Object> config);

}
