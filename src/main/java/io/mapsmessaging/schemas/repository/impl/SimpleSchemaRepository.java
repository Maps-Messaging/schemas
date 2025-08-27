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

package io.mapsmessaging.schemas.repository.impl;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.repository.SchemaRepository;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The type Simple schema repository.
 */
public class SimpleSchemaRepository implements SchemaRepository {

  private final Map<String, List<SchemaConfig>> mapByContext;
  protected final Map<String, SchemaConfig> mapByUUID;


  /**
   * Instantiates a new Simple schema repository.
   */
  public SimpleSchemaRepository() {
    mapByContext = new LinkedHashMap<>();
    mapByUUID = new LinkedHashMap<>();
  }

  @Override
  public SchemaConfig addSchema(@NonNull String context, @NonNull SchemaConfig config) {
    SchemaConfig existing = mapByUUID.get(config.getUniqueId());
    if (existing != null) {
      config = existing;
    } else {
      mapByUUID.put(config.getUniqueId(), config);
    }
    List<SchemaConfig> list = mapByContext.computeIfAbsent(context, k -> new ArrayList<>());
    list.add(config);
    return config;
  }

  @Override
  public SchemaConfig getSchema(@NonNull String uuid) {
    return mapByUUID.get(uuid);
  }

  @Override
  public @NonNull List<SchemaConfig> getSchemaByContext(@NonNull String context) {
    List<SchemaConfig> response = mapByContext.get(context);
    if (response == null) {
      response = new ArrayList<>();
    }
    return response;
  }

  @Override
  public List<SchemaConfig> getSchemas(@NonNull String type) {
    List<SchemaConfig> matching = new ArrayList<>();
    Stream<SchemaConfig> filteredStream = mapByUUID.values().stream().filter(schemaConfig -> schemaConfig.getFormat().equalsIgnoreCase(type));
    filteredStream.forEach(matching::add);
    return matching;
  }

  @Override
  public @NonNull List<SchemaConfig> getAll() {
    return new ArrayList<>(mapByUUID.values());
  }

  @Override
  public @NonNull Map<String, List<SchemaConfig>> getMappedSchemas() {
    return new LinkedHashMap<>(mapByContext);
  }

  @Override
  public void removeSchema(@NonNull String uuid) {
    SchemaConfig config = mapByUUID.remove(uuid);
    if (config != null) {
      for (List<SchemaConfig> list : mapByContext.values()) {
        list.remove(config);
      }
    }
  }

  @Override
  public void removeAllSchemas() {
    mapByUUID.clear();
    mapByContext.clear();
  }
}
