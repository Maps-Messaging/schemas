package io.mapsmessaging.schemas.repository.impl;

import io.mapsmessaging.schemas.model.XRegistrySchemaResource;
import io.mapsmessaging.schemas.model.XRegistrySchemaVersion;
import io.mapsmessaging.schemas.repository.SchemaRepository;
import io.mapsmessaging.schemas.repository.impl.xregistry.XRegistryClient;
import io.mapsmessaging.schemas.repository.impl.xregistry.XRegistryConfig;
import lombok.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * xRegistry-backed repository with on-disk caching.
 * Extends FileSchemaRepository for local persistence and offline use.
 */
public class XRegistrySchemaRepository extends FileSchemaRepository implements SchemaRepository {

  private final XRegistryClient client;

  public XRegistrySchemaRepository(@NonNull File rootDirectory, @NonNull XRegistryConfig config) throws IOException {
    super(rootDirectory);
    this.client = new XRegistryClient(config);
  }

  // ---------------------------- API ----------------------------

  @Override
  public XRegistrySchemaResource createSchema(@NonNull String schemaId, XRegistrySchemaVersion initialVersion) {
    XRegistrySchemaResource remote = client.createSchema(schemaId, initialVersion);
    // hydrate cache from remote canonical state
    cacheFromRemote(remote);
    return super.getResource(schemaId);
  }

  @Override
  public XRegistrySchemaResource getResource(@NonNull String schemaId) {
    // try remote first; if unavailable, serve from cache
    XRegistrySchemaResource remote = client.safeGetResource(schemaId);
    if (remote != null) {
      cacheFromRemote(remote);
      return remote;
    }
    return super.getResource(schemaId);
  }

  @Override
  public XRegistrySchemaVersion getVersion(@NonNull String schemaId, @NonNull String versionId) {
    XRegistrySchemaVersion remote = client.safeGetVersion(schemaId, versionId);
    if (remote != null) {
      // write-through to cache
      super.addVersion(schemaId, remote);
      return remote;
    }
    return super.getVersion(schemaId, versionId);
  }

  @Override
  public XRegistrySchemaVersion addVersion(@NonNull String schemaId, @NonNull XRegistrySchemaVersion version) {
    XRegistrySchemaVersion created = client.addVersion(schemaId, version);
    // write-through to cache
    return super.addVersion(schemaId, created);
  }

  @Override
  public XRegistrySchemaResource setDefaultVersion(@NonNull String schemaId, @NonNull String versionId) {
    XRegistrySchemaResource remote = client.setDefaultVersion(schemaId, versionId);
    cacheFromRemote(remote);
    return super.getResource(schemaId);
  }

  @Override
  public List<XRegistrySchemaVersion> listVersions(@NonNull String schemaId, int page, int size) {
    List<XRegistrySchemaVersion> remote = client.safeListVersions(schemaId, page, size);
    if (remote != null) {
      for (XRegistrySchemaVersion v : remote) {
        super.addVersion(schemaId, v);
      }
      return remote;
    }
    return super.listVersions(schemaId, page, size);
  }

  @Override
  public List<XRegistrySchemaResource> search(String format, Map<String, String> labelFilter, int page, int size) {
    List<XRegistrySchemaResource> remote = client.safeSearch(format, labelFilter, page, size);
    if (remote != null) {
      // gently refresh cache with rows’ defaults
      for (XRegistrySchemaResource r : remote) cacheFromRemote(r);
      return remote;
    }
    return super.search(format, labelFilter, page, size);
  }

  @Override
  public XRegistrySchemaResource updateMetadata(@NonNull String schemaId,
                                                String documentation,
                                                Map<String, String> labels,
                                                Map<String, Object> meta) {
    XRegistrySchemaResource remote = client.updateMetadata(schemaId, documentation, labels, meta);
    cacheFromRemote(remote);
    return super.getResource(schemaId);
  }

  @Override
  public boolean deleteVersion(@NonNull String schemaId, @NonNull String versionId, boolean force) {
    boolean ok = client.deleteVersion(schemaId, versionId, force);
    if (!ok) return false;
    return super.deleteVersion(schemaId, versionId, force);
  }

  @Override
  public boolean deleteSchema(@NonNull String schemaId, boolean force) {
    boolean ok = client.deleteSchema(schemaId, force);
    if (!ok) return false;
    return super.deleteSchema(schemaId, force);
  }

  // ---------------------------- Cache helpers ----------------------------

  private void cacheFromRemote(XRegistrySchemaResource remote) {
    if (remote == null) return;
    // ensure local resource exists
    XRegistrySchemaResource local = super.getResource(remote.getSchemaId());
    if (local == null) {
      super.createSchema(remote.getSchemaId(), null);
    }
    // versions map
    if (remote.getVersions() != null && !remote.getVersions().isEmpty()) {
      for (Map.Entry<String, XRegistrySchemaVersion> e : remote.getVersions().entrySet()) {
        super.addVersion(remote.getSchemaId(), e.getValue());
      }
    }
    // default pointer
    if (remote.getVersionId() != null) {
      super.setDefaultVersion(remote.getSchemaId(), remote.getVersionId());
    }
    // meta/doc/labels on default
    if (remote.getDefaultVersion() != null) {
      Map<String, String> labels = remote.getDefaultVersion().getLabels();
      super.updateMetadata(
          remote.getSchemaId(),
          remote.getDefaultVersion().getDocumentation(),
          labels != null ? new LinkedHashMap<>(labels) : null,
          remote.getMeta());
    } else if (remote.getMeta() != null) {
      super.updateMetadata(remote.getSchemaId(), null, null, remote.getMeta());
    }
  }
}
