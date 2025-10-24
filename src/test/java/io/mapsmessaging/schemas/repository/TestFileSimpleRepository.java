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

import com.google.gson.JsonObject;
import io.mapsmessaging.schemas.model.XRegistrySchemaResource;
import io.mapsmessaging.schemas.model.XRegistrySchemaVersion;
import io.mapsmessaging.schemas.repository.impl.FileSchemaRepository;
import io.mapsmessaging.schemas.repository.impl.SimpleSchemaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

class TestFileSchemaRepository extends TestSchemaRepository {

  private static final File ROOT = new File("./test/report");

  @Override
  protected SimpleSchemaRepository getRepository() throws IOException {
    return new FileSchemaRepository(ROOT);
  }

  @AfterEach
  void clearRepository() throws IOException {
    SimpleSchemaRepository repo = getRepository();
    for (io.mapsmessaging.schemas.model.XRegistrySchemaResource r
        : repo.search(null, null, 0, Integer.MAX_VALUE)) {
      repo.deleteSchema(r.getSchemaId(), true);
    }
    deleteDir(ROOT);
  }


  @Test
  void testReload() throws IOException {
    SimpleSchemaRepository repo = getRepository();

    List<String> schemaIds = new ArrayList<>();
    for (int x = 0; x < 10; x++) {
      String schemaId = "repo.reload." + x;
      schemaIds.add(schemaId);

      // create and add a default version
      repo.createSchema(schemaId, null);
      XRegistrySchemaVersion v = jsonVersion("v1");
      v.setEpoch(1L);
      XRegistrySchemaVersion created = repo.addVersion(schemaId, v);
      repo.setDefaultVersion(schemaId, created.getVersionId());

      XRegistrySchemaResource r = repo.getResource(schemaId);
      Assertions.assertNotNull(r);
      Assertions.assertEquals(created.getVersionId(), r.getVersionId());
      Assertions.assertNotNull(r.getDefaultVersion());
    }

    // reload from disk into a fresh repo
    SimpleSchemaRepository reloaded = getRepository();
    Assertions.assertNotEquals(reloaded, repo);

    for (String id : schemaIds) {
      XRegistrySchemaResource r = reloaded.getResource(id);
      Assertions.assertNotNull(r, "missing resource after reload: " + id);
      Assertions.assertNotNull(r.getVersionId(), "missing default after reload: " + id);
      Assertions.assertNotNull(r.getDefaultVersion(), "missing inlined version after reload: " + id);
      Assertions.assertEquals("v1", r.getDefaultVersion().getName());
      Assertions.assertEquals("JSON", r.getDefaultVersion().getFormat());
    }
  }

  // ---- helpers ----

  private static XRegistrySchemaVersion jsonVersion(String name) {
    XRegistrySchemaVersion v = new XRegistrySchemaVersion();
    v.setName(name);
    v.setFormat("JSON");
    JsonObject schema = new JsonObject();
    schema.addProperty("type", "object");
    v.setSchema(schema);
    OffsetDateTime now = OffsetDateTime.now();
    v.setCreatedAt(now);
    v.setModifiedAt(now);
    return v;
  }

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
