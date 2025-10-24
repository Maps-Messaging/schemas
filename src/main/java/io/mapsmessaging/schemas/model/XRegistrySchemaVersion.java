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

package io.mapsmessaging.schemas.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
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
public class XRegistrySchemaVersion {

  public static final Gson gson = new GsonBuilder()
      .setPrettyPrinting()
      .disableHtmlEscaping()
      .create();

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
  @SerializedName("createdat")
  private OffsetDateTime createdAt;

  @JsonAdapter(OffsetDateTimeAdapter.class)
  @SerializedName("modifiedat")
  private OffsetDateTime modifiedAt;


  protected XRegistrySchemaVersion(String format) {
    this.format = format;
  }

  public XRegistrySchemaVersion(XRegistrySchemaVersion copyFrom) {
    this.versionId = copyFrom.versionId;
    this.epoch = copyFrom.epoch;
    this.name = copyFrom.name;
    this.description = copyFrom.description;
    this.documentation = copyFrom.documentation;
    this.createdAt = copyFrom.createdAt;
    this.modifiedAt = copyFrom.modifiedAt;
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
      this.format = format.toUpperCase();
    }
  }

  public String getUniqueId() {
    return getVersionId();
  }

  public void setUniqueId(UUID uuid) {
    setVersionId(uuid.toString());
  }

  public byte[] pack() throws IOException {
    if (schema == null && schemaBase64 == null) {
      throw new IOException("Schema or SchemaBase64 are required");
    }
    XRegistrySchemaVersion tmp = new XRegistrySchemaVersion(this);
    return gson.toJson(tmp).getBytes(StandardCharsets.UTF_8);
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


  public OffsetDateTime getExpiresAfter() {
    String dt = getFromLabels("expiresAfter");
    if (dt != null) {
      return OffsetDateTime.parse(dt);
    }
    return null;
  }

  public void setExpiresAfter(OffsetDateTime offsetDateTime) {
    setInLables("expiresAfter", offsetDateTime.toString());
  }

  public OffsetDateTime getNotBefore() {
    String dt = getFromLabels("notBefore");
    if (dt != null) {
      return OffsetDateTime.parse(dt);
    }
    return null;
  }

  public void setNotBefore(OffsetDateTime offsetDateTime) {
    setInLables("notBefore", offsetDateTime.toString());
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
    return getFromLabels("version");
  }

  public void setVersion(String number) {
    setInLables("version", number);
  }
}
