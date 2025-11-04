package io.mapsmessaging.schemas.repository.impl.xregistry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mapsmessaging.schemas.model.SchemaResource;
import io.mapsmessaging.schemas.model.XRegistrySchemaVersion;
import lombok.NonNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Lean xRegistry HTTP client matching the Resource/Version API we defined.
 * - Flat JSON on the wire (spec style).
 * - Small helpers for GET/POST/PUT/DELETE.
 * - "safe*" variants return null on failure (cache can take over).
 */
public class XRegistryClient implements AutoCloseable {

  private static final String JSON = "application/json";
  private static final TypeReference<List<SchemaResource>> RES_LIST = new TypeReference<>() {
  };
  private static final TypeReference<List<XRegistrySchemaVersion>> VER_LIST = new TypeReference<>() {
  };

  private final XRegistryConfig cfg;
  private final HttpClient http;
  private final ObjectMapper om;

  public XRegistryClient(@NonNull XRegistryConfig cfg) {
    this.cfg = cfg;
    this.http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .version(HttpClient.Version.HTTP_1_1)
        .build();
    this.om = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  // ---------- Public API (hard-fail) ----------

  private static String u(String s) {
    return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
  }

  public SchemaResource createSchema(String schemaId, XRegistrySchemaVersion initialVersion) {
    String path = String.format("/groups/%s/schemas/%s", cfg.getGroupName(), u(schemaId));
    // Allow empty body; server may treat it as “create container only”
    return doPost(path, initialVersion == null ? Map.of() : initialVersion, SchemaResource.class);
  }

  public SchemaResource getResource(String schemaId) {
    String path = String.format("/groups/%s/schemas/%s", cfg.getGroupName(), u(schemaId));
    return doGet(path, SchemaResource.class);
  }

  public XRegistrySchemaVersion getVersion(String schemaId, String versionId) {
    String path = String.format("/groups/%s/schemas/%s/versions/%s", cfg.getGroupName(), u(schemaId), u(versionId));
    return doGet(path, XRegistrySchemaVersion.class);
  }

  public XRegistrySchemaVersion addVersion(String schemaId, XRegistrySchemaVersion version) {
    String path = String.format("/groups/%s/schemas/%s/versions", cfg.getGroupName(), u(schemaId));
    return doPost(path, version, XRegistrySchemaVersion.class);
  }

  public SchemaResource setDefaultVersion(String schemaId, String versionId) {
    String path = String.format("/groups/%s/schemas/%s/default", cfg.getGroupName(), u(schemaId));
    return doPut(path, Map.of("versionId", versionId), SchemaResource.class);
  }

  public List<XRegistrySchemaVersion> listVersions(String schemaId, int page, int size) {
    String path = String.format("/groups/%s/schemas/%s/versions?page=%d&size=%d",
        cfg.getGroupName(), u(schemaId), Math.max(0, page), Math.max(1, size));
    return doGet(path, VER_LIST);
  }

  public List<SchemaResource> search(String format, Map<String, String> labels, int page, int size) {
    StringBuilder sb = new StringBuilder()
        .append(String.format("/groups/%s/schemas?page=%d&size=%d", cfg.getGroupName(), Math.max(0, page), Math.max(1, size)));
    if (format != null && !format.isBlank()) sb.append("&format=").append(u(format));
    if (labels != null) {
      for (var e : labels.entrySet()) {
        sb.append("&label=").append(u(e.getKey())).append("%3D").append(u(String.valueOf(e.getValue())));
      }
    }
    return doGet(sb.toString(), RES_LIST);
  }

  public SchemaResource updateMetadata(String schemaId,
                                       String documentation,
                                       Map<String, String> labels,
                                       Map<String, Object> meta) {
    String path = String.format("/groups/%s/schemas/%s/meta", cfg.getGroupName(), u(schemaId));
    return doPatch(path, Map.of(
        "documentation", documentation,
        "labels", labels,
        "meta", meta
    ), SchemaResource.class);
  }

  public boolean deleteVersion(String schemaId, String versionId, boolean force) {
    String path = String.format("/groups/%s/schemas/%s/versions/%s?force=%s",
        cfg.getGroupName(), u(schemaId), u(versionId), force);
    doDelete(path);
    return true;
  }

  // ---------- Safe wrappers (return null on failure) ----------

  public boolean deleteSchema(String schemaId, boolean force) {
    String path = String.format("/groups/%s/schemas/%s?force=%s", cfg.getGroupName(), u(schemaId), force);
    doDelete(path);
    return true;
  }

  public SchemaResource safeGetResource(String schemaId) {
    try {
      return getResource(schemaId);
    } catch (Exception ignore) {
      return null;
    }
  }

  public XRegistrySchemaVersion safeGetVersion(String schemaId, String versionId) {
    try {
      return getVersion(schemaId, versionId);
    } catch (Exception ignore) {
      return null;
    }
  }

  public List<XRegistrySchemaVersion> safeListVersions(String schemaId, int page, int size) {
    try {
      return listVersions(schemaId, page, size);
    } catch (Exception ignore) {
      return null;
    }
  }

  // ---------- HTTP helpers ----------

  public List<SchemaResource> safeSearch(String format, Map<String, String> labels, int page, int size) {
    try {
      return search(format, labels, page, size);
    } catch (Exception ignore) {
      return null;
    }
  }

  private <T> T doGet(String path, Class<T> type) {
    HttpRequest req = base(path).GET().build();
    return exec(req, type);
  }

  private <T> T doGet(String path, TypeReference<T> type) {
    HttpRequest req = base(path).GET().build();
    return exec(req, type);
  }

  private <T> T doPost(String path, Object body, Class<T> type) {
    HttpRequest req = base(path)
        .POST(HttpRequest.BodyPublishers.ofString(write(body), StandardCharsets.UTF_8))
        .build();
    return exec(req, type);
  }

  private <T> T doPut(String path, Object body, Class<T> type) {
    HttpRequest req = base(path)
        .PUT(HttpRequest.BodyPublishers.ofString(write(body), StandardCharsets.UTF_8))
        .build();
    return exec(req, type);
  }

  private <T> T doPatch(String path, Object body, Class<T> type) {
    HttpRequest req = base(path)
        .method("PATCH", HttpRequest.BodyPublishers.ofString(write(body), StandardCharsets.UTF_8))
        .build();
    return exec(req, type);
  }

  private void doDelete(String path) {
    HttpRequest req = base(path).DELETE().build();
    exec(req, Void.class);
  }

  private HttpRequest.Builder base(String path) {
    String base = cfg.getBaseUrl();
    if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
    if (!path.startsWith("/")) path = "/" + path;
    HttpRequest.Builder b = HttpRequest.newBuilder()
        .uri(URI.create(base + path))
        .timeout(Duration.ofSeconds(30))
        .header("Accept", JSON)
        .header("Content-Type", JSON);
    String apiKey = cfg.getApiKey();
    if (apiKey != null && !apiKey.isBlank()) {
      b.header("Authorization", "Bearer " + apiKey);
    }
    return b;
  }

  private String write(Object o) {
    try {
      return om.writeValueAsString(o == null ? Map.of() : o);
    } catch (Exception e) {
      throw new RuntimeException("serialize failed", e);
    }
  }

  private <T> T exec(HttpRequest req, Class<T> type) {
    try {
      HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      int code = r.statusCode();
      if (code == 204 || type == Void.class) return null;
      if (code >= 200 && code < 300) {
        return om.readValue(r.body(), type);
      }
      if (code == 404) throw new RuntimeException("not found: " + req.uri());
      throw new RuntimeException("http " + code + " for " + req.uri() + " body: " + r.body());
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("interrupted " + req.uri(), ie);
    } catch (Exception e) {
      throw new RuntimeException("request failed " + req.uri(), e);
    }
  }

  private <T> T exec(HttpRequest req, TypeReference<T> type) {
    try {
      HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      int code = r.statusCode();
      if (code >= 200 && code < 300) {
        return om.readValue(r.body(), type);
      }
      if (code == 404) throw new RuntimeException("not found: " + req.uri());
      throw new RuntimeException("http " + code + " for " + req.uri() + " body: " + r.body());
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("interrupted " + req.uri(), ie);
    } catch (Exception e) {
      throw new RuntimeException("request failed " + req.uri(), e);
    }
  }

  @Override
  public void close() {
    // nothing to close for JDK HttpClient
  }
}
