package io.mapsmessaging.schemas.repository;

import io.mapsmessaging.schemas.config.impl.JsonSchemaConfig;
import io.mapsmessaging.schemas.repository.impl.FileSchemaRepository;
import io.mapsmessaging.schemas.repository.impl.SimpleSchemaRepository;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestFileSimpleRepository extends TestSimpleRepository {

  protected SimpleSchemaRepository getRepository() throws IOException {
    return new FileSchemaRepository(new File("./test/report"));
  }

  @AfterEach
  void clearRepository() throws IOException {
    getRepository().removeAllSchemas();
  }


  @Test
  void testReload() throws IOException {
    SimpleSchemaRepository repository = getRepository();
    for (int x = 0; x < 10; x++) {
      JsonSchemaConfig json = new JsonSchemaConfig();
      json.setUniqueId(UUID.randomUUID());
      repository.addSchema("/root/json/" + x, json);
      Assertions.assertNotNull(repository.getSchema(json.getUniqueId()));
      Assertions.assertEquals(json, repository.getSchema(json.getUniqueId()));

      Assertions.assertNotNull(repository.getSchemaByContext("/root/json/" + x));
      Assertions.assertEquals(json, repository.getSchemaByContext("/root/json/" + x).get(0));
    }
    // we should have 10, so lets load up a new repo
    SimpleSchemaRepository repositoryReload = getRepository();
    Assertions.assertNotEquals(repositoryReload, repository);
    Assertions.assertEquals(repositoryReload.getAll().size(), repository.getAll().size());
  }
}
