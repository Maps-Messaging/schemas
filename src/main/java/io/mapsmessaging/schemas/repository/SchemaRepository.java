package io.mapsmessaging.schemas.repository;

import io.mapsmessaging.schemas.config.SchemaConfig;
import java.util.List;

/**
 * The interface Schema repository.
 */
public interface SchemaRepository {

  /**
   * Add schema schema config.
   *
   * @param context the context
   * @param config the config
   * @return the schema config
   */
  SchemaConfig addSchema(String context, SchemaConfig config);

  /**
   * Gets schema.
   *
   * @param uuid the uuid
   * @return the schema
   */
  SchemaConfig getSchema(String uuid);

  /**
   * Gets schema by context.
   *
   * @param context the context
   * @return the schema by context
   */
  List<SchemaConfig> getSchemaByContext(String context);

  /**
   * Gets schemas.
   *
   * @param type the type
   * @return the schemas
   */
  List<SchemaConfig> getSchemas(String type);

  /**
   * Gets all.
   *
   * @return the all
   */
  List<SchemaConfig> getAll();

  /**
   * Remove schema.
   *
   * @param uuid the uuid
   */
  void removeSchema(String uuid);

  /**
   * Remove all schemas.
   */
  void removeAllSchemas();
}
