// RestSchemaRepository.java
package io.mapsmessaging.schemas.repository.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.mapsmessaging.schemas.model.XRegistrySchemaResource;
import io.mapsmessaging.schemas.model.XRegistrySchemaVersion;
import lombok.NonNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;


public class RestSchemaRepository extends FileSchemaRepository {

  private static final Gson GSON = new GsonBuilder().create();
  private static final String JSON = "application/json";

  private final String baseUrl;
  private final HttpClient httpClient;

  public RestSchemaRepository(@NonNull File rootDirectory, @NonNull String baseUrl) throws IOException {
    super(rootDirectory);
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    this.httpClient = HttpClient.newHttpClient();
  }

  // ---------------------------- Remote helpers ----------------------------

  private HttpRequest.Builder req(String path) {
    return HttpRequest.newBuilder(URI.create(baseUrl + path)).header("Content-Type", JSON).header("Accept", JSON);
  }

  private <T> T tryRemoteGet(String path, Class<T> type) {
    try {
      HttpResponse<String> r = httpClient.send(req(path).GET().build(), HttpResponse.BodyHandlers.ofString());
      if (r.statusCode() >= 200 && r.statusCode() < 300) return GSON.fromJson(r.body(), type);
    } catch (Exception ignored) { /* fallback to cache */ }
    return null;
  }

  private boolean tryRemotePost(String path, Object body) {
    try {
      HttpResponse<String> r = httpClient.send(
          req(path).POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body))).build(),
          HttpResponse.BodyHandlers.ofString());
      return r.statusCode() >= 200 && r.statusCode() < 300;
    } catch (Exception ignored) {
      return false;
    }
  }

  private boolean tryRemotePut(String path, Object body) {
    try {
      HttpResponse<String> r = httpClient.send(
          req(path).PUT(HttpRequest.BodyPublishers.ofString(GSON.toJson(body))).build(),
          HttpResponse.BodyHandlers.ofString());
      return r.statusCode() >= 200 && r.statusCode() < 300;
    } catch (Exception ignored) {
      return false;
    }
  }

  private boolean tryRemoteDelete(String path) {
    try {
      HttpResponse<String> r = httpClient.send(req(path).DELETE().build(), HttpResponse.BodyHandlers.ofString());
      return r.statusCode() >= 200 && r.statusCode() < 300;
    } catch (Exception ignored) {
      return false;
    }
  }

  // ---------------------------- API overrides ----------------------------

  @Override
  public XRegistrySchemaResource createSchema(@NonNull String schemaId, XRegistrySchemaVersion initialVersion) {
    boolean ok = tryRemotePost("/schemas/" + schemaId, initialVersion == null ? new Object() : initialVersion);
    // Even if remote fails, we still create locally (cache-first resilience)
    return super.createSchema(schemaId, initialVersion);
  }

  @Override
  public XRegistrySchemaResource getResource(@NonNull String schemaId) {
    XRegistrySchemaResource remote = tryRemoteGet("/schemas/" + schemaId, XRegistrySchemaResource.class);
    if (remote != null) {
      // hydrate local cache
      if (remote.getVersions() != null) {
        remote.getVersions().values().forEach(v -> super.addVersion(schemaId, v));
      }
      if (remote.getVersionId() != null) super.setDefaultVersion(schemaId, remote.getVersionId());
      return remote;
    }
    return super.getResource(schemaId);
  }

  @Override
  public XRegistrySchemaVersion getVersion(@NonNull String schemaId, @NonNull String versionId) {
    XRegistrySchemaVersion remote = tryRemoteGet("/schemas/" + schemaId + "/versions/" + versionId, XRegistrySchemaVersion.class);
    if (remote != null) {
      super.addVersion(schemaId, remote);
      return remote;
    }
    return super.getVersion(schemaId, versionId);
  }

  @Override
  public XRegistrySchemaVersion addVersion(@NonNull String schemaId, @NonNull XRegistrySchemaVersion version) {
    boolean ok = tryRemotePost("/schemas/" + schemaId + "/versions", version);
    // write-through to cache regardless, we’re the runtime source of truth
    return super.addVersion(schemaId, version);
  }

  @Override
  public XRegistrySchemaResource setDefaultVersion(@NonNull String schemaId, @NonNull String versionId) {
    boolean ok = tryRemotePut("/schemas/" + schemaId + "/default", Map.of("versionId", versionId));
    return super.setDefaultVersion(schemaId, versionId);
  }

  @Override
  public List<XRegistrySchemaVersion> listVersions(@NonNull String schemaId, int page, int size) {
    // keep local paging, remote optional
    return super.listVersions(schemaId, page, size);
  }

  @Override
  public List<XRegistrySchemaResource> search(String format, Map<String, String> labelFilter, int page, int size) {
    // You can call remote if you want, but cache is fine for now
    return super.search(format, labelFilter, page, size);
  }

  @Override
  public XRegistrySchemaResource updateMetadata(@NonNull String schemaId,
                                                String documentation,
                                                Map<String, String> labels,
                                                Map<String, Object> meta) {
    tryRemotePut("/schemas/" + schemaId + "/meta", Map.of(
        "documentation", documentation,
        "labels", labels,
        "meta", meta
    ));
    return super.updateMetadata(schemaId, documentation, labels, meta);
  }

  @Override
  public boolean deleteVersion(@NonNull String schemaId, @NonNull String versionId, boolean force) {
    tryRemoteDelete("/schemas/" + schemaId + "/versions/" + versionId + "?force=" + force);
    return super.deleteVersion(schemaId, versionId, force);
  }

  @Override
  public boolean deleteSchema(@NonNull String schemaId, boolean force) {
    tryRemoteDelete("/schemas/" + schemaId + "?force=" + force);
    return super.deleteSchema(schemaId, force);
  }
}
