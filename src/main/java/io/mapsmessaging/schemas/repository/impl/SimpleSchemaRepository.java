package io.mapsmessaging.schemas.repository.impl;

import io.mapsmessaging.schemas.model.XRegistrySchemaResource;
import io.mapsmessaging.schemas.model.XRegistrySchemaVersion;
import io.mapsmessaging.schemas.repository.SchemaRepository;
import lombok.NonNull;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;

public class SimpleSchemaRepository implements SchemaRepository {

  protected final Map<String, XRegistrySchemaResource> resourcesBySchemaId;

  public SimpleSchemaRepository() {
    resourcesBySchemaId = new LinkedHashMap<>();
  }

  @Override
  public XRegistrySchemaResource createSchema(@NonNull String schemaId, XRegistrySchemaVersion initialVersion) {
    XRegistrySchemaResource existing = resourcesBySchemaId.get(schemaId);
    if (existing != null) {
      return existing;
    }
    XRegistrySchemaResource resource = new XRegistrySchemaResource();
    resource.setSchemaId(schemaId);
    resource.setXid(schemaId); // mirror unless you map externals differently
    resource.setVersions(new LinkedHashMap<>());
    resource.setVersionsCount(0);

    if (initialVersion != null) {
      XRegistrySchemaVersion created = prepareVersionForInsert(initialVersion);
      resource.getVersions().put(created.getVersionId(), created);
      resource.setVersionsCount(1);
      resource.setVersionId(created.getVersionId());
      // inline default
      resource.setDefaultVersion(copyVersion(created));
    }

    resourcesBySchemaId.put(schemaId, resource);
    return resource;
  }

  @Override
  public XRegistrySchemaResource getResource(@NonNull String schemaId) {
    XRegistrySchemaResource resource = resourcesBySchemaId.get(schemaId);
    if (resource == null) {
      return null;
    }
    // ensure inline default mirrors versions map
    String versionId = resource.getVersionId();
    if (versionId != null) {
      XRegistrySchemaVersion v = resource.getVersions() != null ? resource.getVersions().get(versionId) : null;
      if (v != null) {
        resource.setDefaultVersion(copyVersion(v));
      }
    }
    return resource;
  }

  @Override
  public XRegistrySchemaVersion getVersion(@NonNull String schemaId, @NonNull String versionId) {
    XRegistrySchemaResource resource = resourcesBySchemaId.get(schemaId);
    if (resource == null || resource.getVersions() == null) {
      return null;
    }
    XRegistrySchemaVersion v = resource.getVersions().get(versionId);
    if (v == null) {
      return null;
    }
    return copyVersion(v);
  }

  @Override
  public XRegistrySchemaVersion addVersion(@NonNull String schemaId, @NonNull XRegistrySchemaVersion version) {
    XRegistrySchemaResource resource = resourcesBySchemaId.get(schemaId);
    if (resource == null) {
      resource = createSchema(schemaId, null);
    }
    XRegistrySchemaVersion created = prepareVersionForInsert(version);
    if (resource.getVersions().containsKey(created.getVersionId())) {
      // idempotent: return existing
      return copyVersion(resource.getVersions().get(created.getVersionId()));
    }
    resource.getVersions().put(created.getVersionId(), created);
    resource.setVersionsCount(resource.getVersions().size());
    // do not auto-flip default here
    return copyVersion(created);
  }

  @Override
  public XRegistrySchemaResource setDefaultVersion(@NonNull String schemaId, @NonNull String versionId) {
    XRegistrySchemaResource resource = resourcesBySchemaId.get(schemaId);
    if (resource == null) {
      return null;
    }
    if (resource.getVersions() == null || !resource.getVersions().containsKey(versionId)) {
      return null;
    }
    resource.setVersionId(versionId);
    resource.setDefaultVersion(copyVersion(resource.getVersions().get(versionId)));
    return resource;
  }

  @Override
  public List<XRegistrySchemaVersion> listVersions(@NonNull String schemaId, int page, int size) {
    XRegistrySchemaResource resource = resourcesBySchemaId.get(schemaId);
    if (resource == null || resource.getVersions() == null || resource.getVersions().isEmpty()) {
      return List.of();
    }
    List<XRegistrySchemaVersion> all = new ArrayList<>(resource.getVersions().values());
    int from = Math.max(0, page * Math.max(size, 0));
    int to = Math.min(all.size(), from + Math.max(size, 0));
    if (from >= to) {
      return List.of();
    }
    List<XRegistrySchemaVersion> result = new ArrayList<>(to - from);
    for (int i = from; i < to; i++) {
      result.add(copyVersion(all.get(i)));
    }
    return result;
  }

  @Override
  public List<XRegistrySchemaResource> search(String format, Map<String, String> labelFilter, int page, int size) {
    List<XRegistrySchemaResource> rows = new ArrayList<>();
    for (XRegistrySchemaResource resource : resourcesBySchemaId.values()) {
      XRegistrySchemaVersion dv = resolveDefault(resource);
      if (dv == null) {
        continue;
      }
      if (format != null && dv.getFormat() != null && !dv.getFormat().equalsIgnoreCase(format)) {
        continue;
      }
      if (labelFilter != null && !labelFilter.isEmpty()) {
        if (dv.getLabels() == null || !labelsMatch(dv.getLabels(), labelFilter)) {
          continue;
        }
      }
      // ensure inline default is fresh
      XRegistrySchemaResource shallow = shallowCopyResource(resource);
      shallow.setDefaultVersion(copyVersion(dv));
      rows.add(shallow);
    }
    int from = Math.max(0, page * Math.max(size, 0));
    int to = Math.min(rows.size(), from + Math.max(size, 0));
    if (from >= to) {
      return List.of();
    }
    return new ArrayList<>(rows.subList(from, to));
  }

  @Override
  public XRegistrySchemaResource updateMetadata(@NonNull String schemaId,
                                                String documentation,
                                                Map<String, String> labels,
                                                Map<String, Object> meta) {
    XRegistrySchemaResource resource = resourcesBySchemaId.get(schemaId);
    if (resource == null) {
      return null;
    }
    XRegistrySchemaVersion dv = resolveDefault(resource);
    if (dv == null) {
      return resource;
    }
    if (documentation != null) {
      dv.setDocumentation(documentation);
    }
    if (labels != null && !labels.isEmpty()) {
      if (dv.getLabels() == null) {
        dv.setLabels(new LinkedHashMap<>());
      }
      dv.getLabels().putAll(labels);
    }
    if (meta != null && !meta.isEmpty()) {
      if (resource.getMeta() == null) {
        resource.setMeta(new LinkedHashMap<>());
      }
      resource.getMeta().putAll(meta);
    }
    dv.setModifiedAt(OffsetDateTime.now());
    // reflect inline
    resource.setDefaultVersion(copyVersion(dv));
    return resource;
  }

  @Override
  public boolean deleteVersion(@NonNull String schemaId, @NonNull String versionId, boolean force) {
    XRegistrySchemaResource resource = resourcesBySchemaId.get(schemaId);
    if (resource == null || resource.getVersions() == null) {
      return false;
    }
    String currentDefault = resource.getVersionId();
    if (Objects.equals(currentDefault, versionId) && !force) {
      return false;
    }
    XRegistrySchemaVersion removed = resource.getVersions().remove(versionId);
    if (removed == null) {
      return false;
    }
    resource.setVersionsCount(resource.getVersions().size());
    if (Objects.equals(currentDefault, versionId)) {
      resource.setVersionId(null);
      resource.setDefaultVersion(null);
    }
    return true;
  }

  @Override
  public boolean deleteSchema(@NonNull String schemaId, boolean force) {
    XRegistrySchemaResource resource = resourcesBySchemaId.get(schemaId);
    if (resource == null) {
      return false;
    }
    if (!force && resource.getVersions() != null && !resource.getVersions().isEmpty()) {
      return false;
    }
    resourcesBySchemaId.remove(schemaId);
    return true;
  }

  private XRegistrySchemaVersion prepareVersionForInsert(@NonNull XRegistrySchemaVersion version) {
    XRegistrySchemaVersion v = copyVersion(version);
    if (v.getVersionId() == null || v.getVersionId().isBlank()) {
      v.setVersionId(UUID.randomUUID().toString());
    }
    OffsetDateTime now = OffsetDateTime.now();
    if (v.getCreatedAt() == null) {
      v.setCreatedAt(now);
    }
    v.setModifiedAt(now);
    // minimal sanity checks
    if (v.getEpoch() != null && v.getEpoch() < 0L) {
      throw new IllegalArgumentException("epoch must be unsigned");
    }
    int present = presentCount(v.getSchema(), v.getSchemaBase64(), v.getSchemaUrl());
    if (present > 0 && (v.getFormat() == null || v.getFormat().isBlank())) {
      throw new IllegalArgumentException("format is required when schema content is present");
    }
    if (present != 1) {
      throw new IllegalArgumentException("exactly one of schema | schemabase64 | schemaurl must be present");
    }
    if (v.getDocumentation() != null) {
      try {
        URI.create(v.getDocumentation());
      } catch (Exception e) {
        // ignore invalid URL in simple repo
      }
    }
    return v;
  }

  private int presentCount(Object schema, String base64, String url) {
    int count = 0;
    if (schema != null) {
      count++;
    }
    if (base64 != null && !base64.isBlank()) {
      count++;
    }
    if (url != null && !url.isBlank()) {
      count++;
    }
    return count;
  }

  private boolean labelsMatch(Map<String, String> labels, Map<String, String> filter) {
    for (Map.Entry<String, String> e : filter.entrySet()) {
      String value = labels.get(e.getKey());
      if (value == null) {
        return false;
      }
      if (!value.equals(e.getValue())) {
        return false;
      }
    }
    return true;
  }

  private XRegistrySchemaVersion resolveDefault(XRegistrySchemaResource resource) {
    String versionId = resource.getVersionId();
    if (versionId == null || resource.getVersions() == null) {
      return null;
    }
    return resource.getVersions().get(versionId);
  }

  private XRegistrySchemaVersion copyVersion(XRegistrySchemaVersion v) {
    // shallow copy; adjust if you need deep copy of schema JsonElement
    XRegistrySchemaVersion c = new XRegistrySchemaVersion();
    c.setVersionId(v.getVersionId());
    c.setEpoch(v.getEpoch());
    c.setName(v.getName());
    c.setDescription(v.getDescription());
    c.setDocumentation(v.getDocumentation());
    c.setLabels(v.getLabels() != null ? new LinkedHashMap<>(v.getLabels()) : null);
    c.setAncestor(v.getAncestor());
    c.setFormat(v.getFormat());
    c.setSchemaUrl(v.getSchemaUrl());
    c.setSchema(v.getSchema());
    c.setSchemaBase64(v.getSchemaBase64());
    c.setCreatedAt(v.getCreatedAt());
    c.setModifiedAt(v.getModifiedAt());
    return c;
  }

  private XRegistrySchemaResource shallowCopyResource(XRegistrySchemaResource r) {
    XRegistrySchemaResource c = new XRegistrySchemaResource();
    c.setSchemaId(r.getSchemaId());
    c.setVersionId(r.getVersionId());
    c.setSelf(r.getSelf());
    c.setXid(r.getXid());
    c.setMetaUrl(r.getMetaUrl());
    c.setMeta(r.getMeta() != null ? new LinkedHashMap<>(r.getMeta()) : null);
    c.setVersionsUrl(r.getVersionsUrl());
    c.setVersionsCount(r.getVersionsCount());
    // do not copy versions map for search rows
    return c;
  }
}
