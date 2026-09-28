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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.mapsmessaging.schemas.config.GsonFactory;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.SchemaResource;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RestSchemaRepositoryCoverageTest {

  @TempDir
  Path tempDirectory;

  @Test
  void remoteGetsHydrateResourcesAndVersions() throws Exception {
    SchemaConfig version = jsonVersion("v1");
    SchemaResource resource = new SchemaResource();
    resource.setSchemaId("remote");
    resource.put("v1", version);
    resource.setDefaultVersion(version);

    String resourceJson = GsonFactory.buildGson().toJson(resource);
    String versionJson = GsonFactory.buildGson().toJson(version);

    try (TestHttpServer server = new TestHttpServer(exchange -> {
      String path = exchange.getRequestURI().getPath();
      if (path.endsWith("/versions/v1")) {
        return new Response(200, versionJson);
      }
      return new Response(200, resourceJson);
    })) {
      RestSchemaRepository repository =
          new RestSchemaRepository(tempDirectory.toFile(), server.baseUrl() + "/");

      SchemaResource remote = repository.getResource("remote");
      assertNotNull(remote);
      assertEquals("remote", remote.getSchemaId());
      assertEquals("v1", remote.getDefaultVersion().getVersion());

      SchemaConfig loaded = repository.getVersion("remote", "v1");
      assertNotNull(loaded);
      assertEquals("v1", loaded.getVersion());

      assertTrue(server.paths().contains("/schemas/remote"));
      assertTrue(server.paths().contains("/schemas/remote/versions/v1"));
    }
  }

  @Test
  void mutationsCallExpectedRemoteVerbsAndRemainAvailableLocally() throws Exception {
    try (TestHttpServer server = new TestHttpServer(exchange -> new Response(200, "{}"))) {
      RestSchemaRepository repository =
          new RestSchemaRepository(tempDirectory.toFile(), server.baseUrl());

      repository.createSchema("schema", null);
      repository.addVersion("schema", jsonVersion("v1"));
      repository.setDefaultVersion("schema", "v1");
      repository.updateMetadata(
          "schema",
          "v1",
          "https://example.invalid/docs",
          Map.of("team", "schema")
      );

      assertEquals("schema", repository.getResource("schema").getSchemaId());
      assertEquals("schema", repository.getResource("schema").get("v1").getLabels().get("team"));

      assertTrue(repository.deleteVersion("schema", "v1", true));

      assertTrue(server.requests().stream().anyMatch(r -> r.equals("POST /schemas/schema")));
      assertTrue(server.requests().stream().anyMatch(r -> r.equals("POST /schemas/schema/versions")));
      assertTrue(server.requests().stream().anyMatch(r -> r.equals("PUT /schemas/schema/default")));
      assertTrue(server.requests().stream().anyMatch(r -> r.equals("PUT /schemas/schema/meta")));
      assertTrue(
          server.requests().stream()
              .anyMatch(r -> r.equals("DELETE /schemas/schema/versions/v1?force=true"))
      );
    }
  }

  @Test
  void nonSuccessRemoteResponsesFallBackToLocalRepository() throws Exception {
    try (TestHttpServer server = new TestHttpServer(exchange -> new Response(503, "{}"))) {
      RestSchemaRepository repository =
          new RestSchemaRepository(tempDirectory.toFile(), server.baseUrl());

      repository.createSchema("local", null);
      repository.addVersion("local", jsonVersion("v1"));

      SchemaResource local = repository.getResource("local");
      SchemaConfig version = repository.getVersion("local", "v1");

      assertNotNull(local);
      assertNotNull(version);
      assertEquals("v1", version.getVersion());
      assertFalse(server.requests().isEmpty());
    }
  }

  @Test
  void unreachableRemoteFallsBackToLocalWrites() throws Exception {
    RestSchemaRepository repository =
        new RestSchemaRepository(tempDirectory.toFile(), "http://127.0.0.1:1");

    repository.createSchema("offline", null);
    SchemaResource resource = repository.addVersion("offline", jsonVersion("v1"));

    assertNotNull(resource.get("v1"));
  }

  private SchemaConfig jsonVersion(String versionId) {
    SchemaConfig config = new SchemaConfig();
    config.setFormat("json");
    config.setVersion(versionId);
    JsonObject schema = new JsonObject();
    schema.addProperty("type", "object");
    config.setSchema(schema);
    return config;
  }

  private record Response(int status, String body) {
  }

  private static final class TestHttpServer implements AutoCloseable {

    private final HttpServer server;
    private final Function<HttpExchange, Response> responder;
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private final List<String> paths = new CopyOnWriteArrayList<>();

    private TestHttpServer(Function<HttpExchange, Response> responder) throws IOException {
      this.responder = responder;
      server = HttpServer.create(
          new InetSocketAddress(InetAddress.getLoopbackAddress(), 0),
          0
      );
      server.createContext("/", this::handle);
      server.start();
    }

    private void handle(HttpExchange exchange) throws IOException {
      String query = exchange.getRequestURI().getRawQuery();
      String path = exchange.getRequestURI().getPath();
      paths.add(path);
      requests.add(
          exchange.getRequestMethod() + " " + path + (query == null ? "" : "?" + query)
      );

      Response response = responder.apply(exchange);
      byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(response.status(), body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    }

    private String baseUrl() {
      return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private List<String> requests() {
      return new ArrayList<>(requests);
    }

    private List<String> paths() {
      return new ArrayList<>(paths);
    }

    @Override
    public void close() {
      server.stop(0);
    }
  }
}
