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

package io.mapsmessaging.schemas.repository;

import io.mapsmessaging.schemas.model.XRegistrySchemaResource;
import io.mapsmessaging.schemas.model.XRegistrySchemaVersion;

import java.util.List;
import java.util.Map;

/**
 * Repository API for schema resources and their versions.
 * Runtime bindings (context → schema) are handled elsewhere.
 */
public interface SchemaRepository {

  /**
   * Create a new schema resource with an optional initial default version.
   *
   * @param schemaId       the schema identifier
   * @param initialVersion the initial version (nullable)
   * @return the created resource
   */
  XRegistrySchemaResource createSchema(String schemaId, XRegistrySchemaVersion initialVersion);

  /**
   * Get a schema resource with the default version inlined.
   *
   * @param schemaId the schema identifier
   * @return the resource or null if not found
   */
  XRegistrySchemaResource getResource(String schemaId);

  /**
   * Get a specific version for a schema.
   *
   * @param schemaId  the schema identifier
   * @param versionId the version identifier
   * @return the version or null if not found
   */
  XRegistrySchemaVersion getVersion(String schemaId, String versionId);

  /**
   * Add a new version to an existing schema.
   *
   * @param schemaId the schema identifier
   * @param version  the version payload
   * @return the created version (with ids and timestamps)
   */
  XRegistrySchemaVersion addVersion(String schemaId, XRegistrySchemaVersion version);

  /**
   * Set the default version for a schema.
   *
   * @param schemaId  the schema identifier
   * @param versionId the version identifier to set as default
   * @return the updated resource
   */
  XRegistrySchemaResource setDefaultVersion(String schemaId, String versionId);

  /**
   * List versions for a schema.
   *
   * @param schemaId the schema identifier
   * @param page     zero-based page index
   * @param size     page size
   * @return versions in the requested page
   */
  List<XRegistrySchemaVersion> listVersions(String schemaId, int page, int size);

  /**
   * Search schemas by format and label predicates.
   *
   * @param format      optional format filter (e.g., AVRO, PROTOBUF)
   * @param labelFilter optional exact-match labels filter
   * @param page        zero-based page index
   * @param size        page size
   * @return matching resources (default version inlined)
   */
  List<XRegistrySchemaResource> search(String format, Map<String, String> labelFilter, int page, int size);

  /**
   * Update resource-level metadata without changing schema bytes.
   *
   * @param schemaId      the schema identifier
   * @param documentation optional documentation URL (nullable to leave unchanged)
   * @param labels        optional labels to upsert (null to leave unchanged)
   * @param meta          optional meta map to upsert (null to leave unchanged)
   * @return the updated resource
   */
  XRegistrySchemaResource updateMetadata(String schemaId, String documentation, Map<String, String> labels, Map<String, Object> meta);

  /**
   * Delete a specific version.
   *
   * @param schemaId  the schema identifier
   * @param versionId the version identifier
   * @param force     if true, allow deleting the current default
   * @return true if deleted
   */
  boolean deleteVersion(String schemaId, String versionId, boolean force);

  /**
   * Delete an entire schema resource.
   *
   * @param schemaId the schema identifier
   * @param force    if true, delete even if versions exist
   * @return true if deleted
   */
  boolean deleteSchema(String schemaId, boolean force);
}
