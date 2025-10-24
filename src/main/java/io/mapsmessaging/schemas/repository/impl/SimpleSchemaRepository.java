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

import io.mapsmessaging.schemas.model.XRegistrySchemaVersion;
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

  protected final Map<String, XRegistrySchemaVersion> mapByUUID;
  private final Map<String, List<XRegistrySchemaVersion>> mapByContext;


  /**
   * Instantiates a new Simple schema repository.
   */
  public SimpleSchemaRepository() {
    mapByContext = new LinkedHashMap<>();
    mapByUUID = new LinkedHashMap<>();
  }

  @Override
  public XRegistrySchemaVersion addSchema(@NonNull String context, @NonNull XRegistrySchemaVersion config) {
    XRegistrySchemaVersion existing = mapByUUID.get(config.getUniqueId());
    if (existing != null) {
      config = existing;
    } else {
      mapByUUID.put(config.getUniqueId(), config);
    }
    List<XRegistrySchemaVersion> list = mapByContext.computeIfAbsent(context, k -> new ArrayList<>());
    list.add(config);
    return config;
  }

  @Override
  public XRegistrySchemaVersion getSchema(@NonNull String uuid) {
    return mapByUUID.get(uuid);
  }

  @Override
  public @NonNull List<XRegistrySchemaVersion> getSchemaByContext(@NonNull String context) {
    List<XRegistrySchemaVersion> response = mapByContext.get(context);
    if (response == null) {
      response = new ArrayList<>();
    }
    return response;
  }

  @Override
  public List<XRegistrySchemaVersion> getSchemas(@NonNull String type) {
    List<XRegistrySchemaVersion> matching = new ArrayList<>();
    Stream<XRegistrySchemaVersion> filteredStream = mapByUUID.values().stream().filter(schemaConfig -> schemaConfig.getFormat().equalsIgnoreCase(type));
    filteredStream.forEach(matching::add);
    return matching;
  }

  @Override
  public @NonNull List<XRegistrySchemaVersion> getAll() {
    return new ArrayList<>(mapByUUID.values());
  }

  @Override
  public @NonNull Map<String, List<XRegistrySchemaVersion>> getMappedSchemas() {
    return new LinkedHashMap<>(mapByContext);
  }

  @Override
  public void removeSchema(@NonNull String uuid) {
    XRegistrySchemaVersion config = mapByUUID.remove(uuid);
    if (config != null) {
      for (List<XRegistrySchemaVersion> list : mapByContext.values()) {
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
