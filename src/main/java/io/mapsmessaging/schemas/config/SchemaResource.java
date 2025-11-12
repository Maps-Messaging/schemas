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

import lombok.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@NoArgsConstructor
@AllArgsConstructor
public class SchemaResource {
  @Getter
  @Setter
  private String schemaId;
  private Map<String, SchemaConfig> versions = new LinkedHashMap<>();
  private String defaultVersion;

  public SchemaResource(SchemaResource r) {
    this.schemaId = r.getSchemaId();
    versions = new LinkedHashMap<>(r.versions);
    defaultVersion = r.defaultVersion;
  }

  public void setDefaultVersion(SchemaConfig schemaConfig) {
    if (!versions.containsKey(schemaConfig.getVersionId())) {
      versions.put(schemaConfig.getVersionId(), schemaConfig);
    }
    defaultVersion = schemaConfig.getVersionId();
  }

  public SchemaConfig getDefaultVersion() {
    if (defaultVersion == null && !versions.isEmpty()) {
      return versions.values().iterator().next();
    }
    return versions.get(defaultVersion);
  }

  public void put(String versionId, SchemaConfig created) {
    created.setUniqueId(schemaId);
    versions.put(versionId, created);
  }

  public SchemaConfig get(@NonNull String versionId) {
    return versions.get(versionId);
  }

  public boolean containsKey(String versionId) {
    return versions.containsKey(versionId);
  }

  public boolean isEmpty() {
    return versions.isEmpty();
  }

  public List<SchemaConfig> getAll() {
    return new ArrayList<>(versions.values());
  }

  public boolean remove(@NonNull String versionId) {
    return versions.remove(versionId) != null;
  }

  public int size() {
    return versions.size();
  }
}
