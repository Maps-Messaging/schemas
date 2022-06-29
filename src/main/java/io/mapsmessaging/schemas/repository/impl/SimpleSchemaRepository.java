package io.mapsmessaging.schemas.repository.impl;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.repository.SchemaRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public class SimpleSchemaRepository implements SchemaRepository {

  private final Map<String, List<SchemaConfig>> mapByContext;
  private final Map<UUID, SchemaConfig> mapByUUID;


  public SimpleSchemaRepository(){
    mapByContext = new LinkedHashMap<>();
    mapByUUID = new LinkedHashMap<>();
  }

  @Override
  public void addSchema(String context, SchemaConfig config) {
    List<SchemaConfig> list = mapByContext.computeIfAbsent(context, k -> new ArrayList<>());
    list.add(config);
    mapByUUID.put(config.getUniqueId(), config);
  }

  @Override
  public SchemaConfig getSchema(UUID uuid) {
    return mapByUUID.get(uuid);
  }

  @Override
  public List<SchemaConfig> getSchema(String context) {
    return mapByContext.get(context);
  }

  @Override
  public List<SchemaConfig> getSchemas(String type) {
    List<SchemaConfig> matching = new ArrayList<>();
    Stream<SchemaConfig> filteredStream = mapByUUID.values().stream().filter(schemaConfig -> schemaConfig.getFormat().equalsIgnoreCase(type));
    filteredStream.forEach(matching::add);
    return matching;
  }

  @Override
  public List<SchemaConfig> getAll() {
    return new ArrayList<>(mapByUUID.values());
  }

  @Override
  public void removeSchema(UUID uuid) {
    SchemaConfig config = mapByUUID.remove(uuid);
    if(config != null){
      for(List<SchemaConfig> list :mapByContext.values()){
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