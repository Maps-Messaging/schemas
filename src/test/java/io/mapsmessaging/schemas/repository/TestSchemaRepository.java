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
import io.mapsmessaging.schemas.repository.impl.SimpleSchemaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;


class TestSchemaRepository {


  protected SimpleSchemaRepository getRepository() throws IOException {
    return new SimpleSchemaRepository();
  }

  private XRegistrySchemaVersion makeJsonVersion(String name) {
    XRegistrySchemaVersion v = new XRegistrySchemaVersion();
    v.setName(name);
    v.setFormat("JSON");
    JsonObject schema = new JsonObject();
    schema.addProperty("type", "object");
    v.setSchema(schema);
    v.setEpoch(1L);
    v.setCreatedAt(OffsetDateTime.now());
    v.setModifiedAt(v.getCreatedAt());
    return v;
  }

  @Test
  void createGetAddVersionAndDefault() throws IOException {
    var repo = getRepository();

    String schemaId = "sensor.temp";
    // create empty schema, then add a version
    XRegistrySchemaResource created = repo.createSchema(schemaId, null);
    Assertions.assertNotNull(created);
    Assertions.assertEquals(schemaId, created.getSchemaId());
    Assertions.assertNull(created.getVersionId());

    XRegistrySchemaVersion v1 = repo.addVersion(schemaId, makeJsonVersion("v1"));
    Assertions.assertNotNull(v1.getVersionId());

    // not default yet
    XRegistrySchemaResource res = repo.getResource(schemaId);
    Assertions.assertNull(res.getVersionId());

    // set default
    XRegistrySchemaResource updated = repo.setDefaultVersion(schemaId, v1.getVersionId());
    Assertions.assertEquals(v1.getVersionId(), updated.getVersionId());
    Assertions.assertNotNull(updated.getDefaultVersion());
    Assertions.assertEquals("v1", updated.getDefaultVersion().getName());

    // get specific version
    XRegistrySchemaVersion got = repo.getVersion(schemaId, v1.getVersionId());
    Assertions.assertEquals(v1.getVersionId(), got.getVersionId());
  }

  @Test
  void listVersionsAndSearch() throws IOException {
    var repo = getRepository();
    String schemaId = "device.metrics";

    repo.createSchema(schemaId, null);
    XRegistrySchemaVersion v1 = repo.addVersion(schemaId, makeJsonVersion("v1"));
    XRegistrySchemaVersion v2 = repo.addVersion(schemaId, makeJsonVersion("v2"));
    repo.setDefaultVersion(schemaId, v2.getVersionId());

    List<XRegistrySchemaVersion> page = repo.listVersions(schemaId, 0, 10);
    Assertions.assertEquals(2, page.size());

    // add labels to default via metadata update
    XRegistrySchemaResource r = repo.updateMetadata(schemaId, null, Map.of("resource", "sensor", "iface", "tempC"), Map.of("validation", true));
    Assertions.assertEquals("v2", r.getDefaultVersion().getName());
    Assertions.assertEquals("sensor", r.getDefaultVersion().getLabels().get("resource"));

    // search by format and labels
    List<XRegistrySchemaResource> found = repo.search("JSON", Map.of("resource", "sensor"), 0, 50);
    Assertions.assertFalse(found.isEmpty());
    Assertions.assertEquals(schemaId, found.get(0).getSchemaId());
  }

  @Test
  void deleteVersionAndSchema() throws IOException {
    var repo = getRepository();
    String schemaId = "app.events";

    XRegistrySchemaVersion v1 = repo.addVersion(schemaId, makeJsonVersion("v1"));
    XRegistrySchemaVersion v2 = repo.addVersion(schemaId, makeJsonVersion("v2"));
    repo.setDefaultVersion(schemaId, v2.getVersionId());

    // cannot delete default without force=false: should fail
    boolean deletedDefault = repo.deleteVersion(schemaId, v2.getVersionId(), false);
    Assertions.assertFalse(deletedDefault);

    // delete non-default
    boolean deleted = repo.deleteVersion(schemaId, v1.getVersionId(), false);
    Assertions.assertTrue(deleted);
    Assertions.assertEquals(1, repo.listVersions(schemaId, 0, 10).size());

    // now force delete default
    boolean deletedForced = repo.deleteVersion(schemaId, v2.getVersionId(), true);
    Assertions.assertTrue(deletedForced);
    Assertions.assertTrue(repo.listVersions(schemaId, 0, 10).isEmpty());

    // create again and then delete schema
    repo.addVersion(schemaId, makeJsonVersion("v3"));
    boolean schemaDeleted = repo.deleteSchema(schemaId, true);
    Assertions.assertTrue(schemaDeleted);
    Assertions.assertNull(repo.getResource(schemaId));
  }

  @Test
  void metadataPatchNoSchemaBytesChange() throws IOException {
    var repo = getRepository();
    String schemaId = "fleet.status";

    XRegistrySchemaVersion v1 = repo.addVersion(schemaId, makeJsonVersion("v1"));
    repo.setDefaultVersion(schemaId, v1.getVersionId());

    XRegistrySchemaResource before = repo.getResource(schemaId);
    String beforeId = before.getDefaultVersion().getVersionId();

    XRegistrySchemaResource after = repo.updateMetadata(schemaId, "https://docs/maps/fleet", Map.of("team", "iot"), Map.of("note", "stable"));
    Assertions.assertEquals(beforeId, after.getDefaultVersion().getVersionId());
    Assertions.assertEquals("iot", after.getDefaultVersion().getLabels().get("team"));
  }
}
