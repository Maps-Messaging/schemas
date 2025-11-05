/*
 *  Copyright [ 2020 - 2025 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 */

package io.mapsmessaging.schemas.repository;

import io.mapsmessaging.schemas.config.ConfigHelper;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.SchemaResource;
import io.mapsmessaging.schemas.repository.impl.FileSchemaRepository;
import io.mapsmessaging.schemas.repository.impl.SimpleSchemaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class TestFileSimpleRepository extends TestSchemaRepository {

  private static final File ROOT = new File("./test/report");

  @Override
  protected SimpleSchemaRepository getRepository() throws IOException {
    return new FileSchemaRepository(ROOT);
  }

  @AfterEach
  void clearRepository() throws IOException {
    SimpleSchemaRepository repo = getRepository();
    for (SchemaResource r
        : repo.search(null, null, 0, Integer.MAX_VALUE)) {
      repo.deleteSchema(r.getSchemaId(), true);
    }
    deleteDir(ROOT);
  }

  @Test
  void testReload() throws IOException {
    SimpleSchemaRepository repo = getRepository();
    List<SchemaConfig> all = ConfigHelper.getAll();
    List<String> schemaIds = new ArrayList<>();
    for (SchemaConfig c : all) {
      repo.createSchema(c.getVersion(), c);
      SchemaResource r = repo.getResource(c.getVersion());
      Assertions.assertNotNull(r);
      Assertions.assertNotNull(r.getVersions().get(c.getVersionId()));
      schemaIds.add(r.getSchemaId());
    }

    // reload from disk into a fresh repo
    SimpleSchemaRepository reloaded = getRepository();
    Assertions.assertNotEquals(reloaded, repo);

    for (String id : schemaIds) {
      SchemaResource r = reloaded.getResource(id);
      Assertions.assertNotNull(r, "missing resource after reload: " + id);
      Assertions.assertNotNull(r.getSchemaId(), "missing default after reload: " + id);
      Assertions.assertNotNull(r.getDefaultVersion(), "missing inlined version after reload: " + id);
      String className = r.getDefaultVersion().getClass().getSimpleName().toLowerCase();
      Assertions.assertTrue(className.startsWith(r.getDefaultVersion().getFormat()), "Incorrect load detected: " + className);
    }
  }

  // ---- helpers ----

  private static void deleteDir(File dir) {
    if (dir == null || !dir.exists()) return;
    File[] files = dir.listFiles();
    if (files != null) {
      for (File f : files) {
        if (f.isDirectory()) deleteDir(f);
        else // best-effort
          //noinspection ResultOfMethodCallIgnored
          f.delete();
      }
    }
    //noinspection ResultOfMethodCallIgnored
    dir.delete();
  }
}
