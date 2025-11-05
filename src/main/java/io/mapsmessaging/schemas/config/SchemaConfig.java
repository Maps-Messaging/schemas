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
package io.mapsmessaging.schemas.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.JsonAdapter;
import io.mapsmessaging.schemas.model.OffsetDateTimeAdapter;
import lombok.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;


@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SchemaConfig {

  public static final Gson gson = GsonFactory.buildGson();

  private String versionId;
  private Long epoch;
  private String name;
  private String description;
  private String documentation;
  private Map<String, String> labels;
  private String ancestor;
  private String format;
  private String schemaUrl;
  private JsonObject schema;
  private String schemaBase64;

  @JsonAdapter(OffsetDateTimeAdapter.class)
  private OffsetDateTime createdAt;

  @JsonAdapter(OffsetDateTimeAdapter.class)
  private OffsetDateTime modifiedAt;

  @JsonAdapter(OffsetDateTimeAdapter.class)
  private OffsetDateTime notBefore;

  @JsonAdapter(OffsetDateTimeAdapter.class)
  private OffsetDateTime expiresAfter;


  protected SchemaConfig(String format) {
    this.format = format;
  }

  public SchemaConfig(SchemaConfig copyFrom) {
    this.versionId = copyFrom.versionId;
    this.epoch = copyFrom.epoch;
    this.name = copyFrom.name;
    this.description = copyFrom.description;
    this.documentation = copyFrom.documentation;
    this.createdAt = copyFrom.createdAt;
    this.modifiedAt = copyFrom.modifiedAt;
    this.notBefore = copyFrom.notBefore;
    this.expiresAfter = copyFrom.expiresAfter;
    this.ancestor = copyFrom.ancestor;
    this.format = copyFrom.format;
    this.schemaUrl = copyFrom.schemaUrl;
    this.schema = copyFrom.schema;
    this.schemaBase64 = copyFrom.schemaBase64;
    if (copyFrom.labels != null) {
      this.labels = new LinkedHashMap<>(copyFrom.labels);
    }
  }

  public void setFormat(String format) {
    if (this.format == null && format != null) {
      this.format = format.toLowerCase();
    }
  }

  public String getUniqueId() {
    return getFromLabels("uniqueId");
  }

  public void setUniqueId(UUID uuid) {
    setInLables("uniqueId", uuid.toString());
  }

  public byte[] pack() throws IOException {
    if (schema == null && schemaBase64 == null) {
      throw new IOException("Schema or SchemaBase64 are required");
    }
    String json = gson.toJson(this);
    return json.getBytes(StandardCharsets.UTF_8);
  }

  public JsonObject packData() {
    return gson.toJsonTree(this).getAsJsonObject();
  }

  public String getInterfaceDescription() {
    return getFromLabels("interface");
  }

  public void setInterfaceDescription(String interfaceDescription) {
    setInLables("interface", interfaceDescription);
  }

  public String getResourceType() {
    return getFromLabels("resource");
  }

  public void setResourceType(String resourceType) {
    setInLables("resource", resourceType);
  }

  public String getComments() {
    return getFromLabels("comments");
  }

  public void setComments(String comments) {
    setInLables("comments", comments);
  }

  public String getSource() {
    return getFromLabels("source");
  }

  public void setSource(String s) {
    setInLables("source", s);
  }

  private String getFromLabels(String key) {
    if (labels != null && !labels.isEmpty()) {
      return labels.get(key);
    }
    return null;
  }

  private void setInLables(String key, String value) {
    if (labels == null) labels = new LinkedHashMap<>();
    labels.put(key, value);
  }

  public String getVersion() {
    return versionId;
  }

  public void setVersion(String number) {
    versionId = number;
  }


  public void setVersion(int val) {
    setVersion("" + val);
  }

  public void setTitle(String val) {
    name = val;
  }

  public String getTitle() {
    return name;
  }

  public SchemaConfig getInstance(SchemaConfig config) {
    return null;
  }

  public String getMimeType() {
    return null;
  }

}
