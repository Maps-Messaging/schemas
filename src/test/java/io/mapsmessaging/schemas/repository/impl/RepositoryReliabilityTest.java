/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 *
 */

package io.mapsmessaging.schemas.repository.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import io.mapsmessaging.schemas.config.SchemaConfig;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepositoryReliabilityTest {

  @TempDir
  Path tempDirectory;

  @Test
  void deletingVersionFromMissingSchemaReturnsFalse() {
    SimpleSchemaRepository repository = new SimpleSchemaRepository();

    assertFalse(repository.deleteVersion("missing", "v1", false));
  }

  @Test
  void simpleRepositoryUsesUtcTimestamps() {
    SimpleSchemaRepository repository = new SimpleSchemaRepository();
    SchemaConfig config = schema("v1");

    repository.addVersion("schema", config);

    SchemaConfig stored = repository.getResource("schema").get("v1");
    assertEquals(ZoneOffset.UTC, stored.getCreatedAt().getOffset());
    assertEquals(ZoneOffset.UTC, stored.getModifiedAt().getOffset());

    repository.updateMetadata("schema", "v1", "https://example.invalid/schema", Map.of());
    assertEquals(
        ZoneOffset.UTC,
        repository.getResource("schema").get("v1").getModifiedAt().getOffset()
    );
  }

  @Test
  void restRepositoryPreservesInterruptStatusWhenRemoteCallIsInterrupted() throws Exception {
    RestSchemaRepository repository =
        new RestSchemaRepository(tempDirectory.toFile(), "http://127.0.0.1:1");

    Thread.currentThread().interrupt();
    try {
      repository.createSchema("schema", null);
      assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void restOverridesMatchParentSynchronizationContract() throws Exception {
    assertSynchronized("createSchema", String.class, SchemaConfig.class);
    assertSynchronized("addVersion", String.class, SchemaConfig.class);
    assertSynchronized("setDefaultVersion", String.class, String.class);
    assertSynchronized("listVersions", String.class, int.class, int.class);
    assertSynchronized("search", String.class, Map.class, int.class, int.class);
    assertSynchronized(
        "updateMetadata",
        String.class,
        String.class,
        String.class,
        Map.class
    );
    assertSynchronized("deleteVersion", String.class, String.class, boolean.class);
  }

  private void assertSynchronized(String methodName, Class<?>... parameterTypes) throws Exception {
    Method method = RestSchemaRepository.class.getMethod(methodName, parameterTypes);
    assertTrue(Modifier.isSynchronized(method.getModifiers()), methodName);
  }

  private SchemaConfig schema(String version) {
    SchemaConfig config = new SchemaConfig();
    config.setVersion(version);
    config.setFormat("json");
    JsonObject schema = new JsonObject();
    schema.addProperty("type", "object");
    config.setSchema(schema);
    return config;
  }
}
