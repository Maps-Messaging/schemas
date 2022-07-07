package io.mapsmessaging.schemas.repository;

import io.mapsmessaging.schemas.config.SchemaConfig;
import java.util.List;

public interface SchemaRepository {

  SchemaConfig addSchema(String context, SchemaConfig config);

  SchemaConfig getSchema(String uuid);

  List<SchemaConfig> getSchemaByContext(String context);

  List<SchemaConfig> getSchemas(String type);

  List<SchemaConfig> getAll();

  void removeSchema(String uuid);

  void removeAllSchemas();
}
