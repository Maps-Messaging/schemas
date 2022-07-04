package io.mapsmessaging.schemas.repository;

import io.mapsmessaging.schemas.config.SchemaConfig;
import java.util.List;
import java.util.UUID;

public interface SchemaRepository {

  SchemaConfig addSchema(String context, SchemaConfig config);

  SchemaConfig getSchema(UUID uuid);

  List<SchemaConfig> getSchema(String context);

  List<SchemaConfig> getSchemas(String type);

  List<SchemaConfig> getAll();

  void removeSchema(UUID uuid);

  void removeAllSchemas();
}
