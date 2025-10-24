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
import lombok.*;

import java.time.OffsetDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class XRegistrySchemaGroup {
  @JsonProperty("schemagroupid")
  private String schemaGroupId;

  @JsonProperty("self")
  private String self;

  @JsonProperty("xid")
  private String xid;

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

  @JsonProperty("schemasurl")
  private String schemasUrl;

  @JsonProperty("schemascount")
  private Integer schemasCount;

  @JsonProperty("schemas")
  private Map<String, XRegistrySchemaResource> schemas;
}
