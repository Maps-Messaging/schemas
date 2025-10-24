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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class XRegistrySchemaResource {
  @JsonProperty("schemaid")
  private String schemaId;

  @JsonProperty("versionid")
  private String versionId;

  @JsonProperty("self")
  private String self;

  @JsonProperty("xid")
  private String xid;

  // Default Version attributes inlined
  @JsonProperty("epoch")
  private Long epoch;

  @JsonProperty("name")
  private String name;

  @JsonProperty("description")
  private String description;

  @JsonProperty("documentation")
  private String documentation;

  @JsonProperty("labels")
  private Map<String, String> labels;

  @JsonProperty("createdat")
  private OffsetDateTime createdAt;

  @JsonProperty("modifiedat")
  private OffsetDateTime modifiedAt;

  @JsonProperty("ancestor")
  private String ancestor;

  @JsonProperty("format")
  private String format;

  @JsonProperty("schemaurl")
  private String schemaUrl;

  // schema can be any JSON; use JsonNode for text/object variants
  @JsonProperty("schema")
  private JsonNode schema;

  // binary payloads encoded as base64
  @JsonProperty("schemabase64")
  private String schemaBase64;

  // Resource-level metadata
  @JsonProperty("metaurl")
  private String metaUrl;

  @JsonProperty("meta")
  private XRegistrySchemaMeta meta;

  // Versions collection (optional)
  @JsonProperty("versionsurl")
  private String versionsUrl;

  @JsonProperty("versionscount")
  private Integer versionsCount;

  @JsonProperty("versions")
  private Map<String, XRegistrySchemaVersion> versions;
}
