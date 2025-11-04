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


import io.mapsmessaging.schemas.config.ConfigHelper;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.model.SchemaResource;
import io.mapsmessaging.schemas.repository.impl.SimpleSchemaRepository;
import io.mapsmessaging.schemas.repository.impl.XRegistrySchemaRepository;
import io.mapsmessaging.schemas.repository.impl.xregistry.XRegistryConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class TestXRegistryRepository {//extends TestSchemaRepository {

  private static final File ROOT = new File("./test/report");


  protected SimpleSchemaRepository getRepository() throws IOException {
    XRegistryConfig config = new XRegistryConfig();
    config.setTimeout(30);
    config.setRetryAttempts(2);
    config.setGroupName("schema");
    config.setCacheTtl(3600);
    config.setBaseUrl("http://localhost:8080");
    config.setEnableCache(true);
    config.setRetryDelay(5);
    return new XRegistrySchemaRepository(ROOT, config);
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


  void testReload() throws IOException {
    SimpleSchemaRepository repo = getRepository();
    List<SchemaConfig> all = ConfigHelper.getAll();
    List<String> schemaIds = new ArrayList<>();
    for (SchemaConfig c : all) {
      repo.createSchema(c.getVersion(), c);
      SchemaResource r = repo.getResource(c.getVersion());
      Assertions.assertNotNull(r);
      Assertions.assertEquals(c.getVersionId(), r.getVersionId());
      Assertions.assertNotNull(r.getVersions().get(c.getVersionId()));
      schemaIds.add(r.getVersionId());
    }

    // reload from disk into a fresh repo
    SimpleSchemaRepository reloaded = getRepository();
    Assertions.assertNotEquals(reloaded, repo);

    for (String id : schemaIds) {
      SchemaResource r = reloaded.getResource(id);
      Assertions.assertNotNull(r, "missing resource after reload: " + id);
      Assertions.assertNotNull(r.getVersionId(), "missing default after reload: " + id);
      Assertions.assertNotNull(r.getDefaultVersion(), "missing inlined version after reload: " + id);
      String className = r.getDefaultVersion().getClass().getSimpleName().toLowerCase();
      Assertions.assertTrue(className.startsWith(r.getDefaultVersion().getFormat()), "Incorrect load detected: " + className);
    }
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
