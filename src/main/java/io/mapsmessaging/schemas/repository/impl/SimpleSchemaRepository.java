package io.mapsmessaging.schemas.repository.impl;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.SchemaConfigFactory;
import io.mapsmessaging.schemas.config.SchemaResource;
import io.mapsmessaging.schemas.repository.SchemaRepository;
import lombok.NonNull;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;

public class SimpleSchemaRepository implements SchemaRepository {

  protected final Map<String, SchemaResource> resourcesBySchemaId;

  public SimpleSchemaRepository() {
    resourcesBySchemaId = new LinkedHashMap<>();
  }

  @Override
  public SchemaResource createSchema(@NonNull String schemaId, SchemaConfig initialVersion) {
    SchemaResource existing = resourcesBySchemaId.get(schemaId);
    if (existing != null) {
      return existing;
    }
    SchemaResource resource = new SchemaResource();
    resource.setSchemaId(schemaId);
    resource.setVersions(new LinkedHashMap<>());

    if (initialVersion != null) {
      SchemaConfig created = prepareVersionForInsert(initialVersion);
      resource.getVersions().put(created.getVersionId(), created);
      // inline default
      resource.setDefaultVersion(copyVersion(created));
    }

    resourcesBySchemaId.put(schemaId, resource);
    return resource;
  }

  @Override
  public SchemaResource getResource(@NonNull String schemaId) {
    return resourcesBySchemaId.get(schemaId);
  }

  @Override
  public boolean deleteResource(String schemaId) {
    return resourcesBySchemaId.remove(schemaId) != null;
  }

  @Override
  public SchemaConfig getVersion(@NonNull String schemaId, @NonNull String versionId) {
    SchemaResource resource = resourcesBySchemaId.get(schemaId);
    if (resource == null || resource.getVersions() == null) {
      return null;
    }
    SchemaConfig v = resource.getVersions().get(versionId);
    if (v == null) {
      return null;
    }
    return copyVersion(v);
  }

  @Override
  public SchemaResource addVersion(@NonNull String schemaId, @NonNull SchemaConfig version) {
    SchemaResource resource = resourcesBySchemaId.get(schemaId);
    if (resource == null) {
      resource = createSchema(schemaId, null);
    }
    SchemaConfig created = prepareVersionForInsert(version);
    if (resource.getVersions().containsKey(created.getVersionId())) {
      // idempotent: return existing
      return resource;
    }
    resource.getVersions().put(created.getVersionId(), created);
    if (resource.getDefaultVersion() == null) {
      resource.setDefaultVersion(created);
    }
    // do not auto-flip default here
    return resource;
  }

  @Override
  public SchemaResource setDefaultVersion(@NonNull String schemaId, @NonNull String versionId) {
    SchemaResource resource = resourcesBySchemaId.get(schemaId);
    if (resource == null) {
      return null;
    }
    if (resource.getVersions() == null || !resource.getVersions().containsKey(versionId)) {
      return null;
    }
    resource.setDefaultVersion(copyVersion(resource.getVersions().get(versionId)));
    return resource;
  }

  @Override
  public List<SchemaConfig> listVersions(@NonNull String schemaId, int page, int size) {
    SchemaResource resource = resourcesBySchemaId.get(schemaId);
    if (resource == null || resource.getVersions() == null || resource.getVersions().isEmpty()) {
      return List.of();
    }
    List<SchemaConfig> all = new ArrayList<>(resource.getVersions().values());
    int from = Math.max(0, page * Math.max(size, 0));
    int to = Math.min(all.size(), from + Math.max(size, 0));
    if (from >= to) {
      return List.of();
    }
    List<SchemaConfig> result = new ArrayList<>(to - from);
    for (int i = from; i < to; i++) {
      result.add(copyVersion(all.get(i)));
    }
    return result;
  }

  @Override
  public List<SchemaResource> search(String format, Map<String, String> labelFilter, int page, int size) {
    List<SchemaResource> rows = new ArrayList<>();
    for (SchemaResource resource : resourcesBySchemaId.values()) {
      SchemaConfig dv = resolveDefault(resource);
      boolean isMatch = match(dv, format, labelFilter);
      isMatch = resource.getVersions().values()
          .stream()
          .anyMatch(v -> match(v, format, labelFilter)) || isMatch;

      if (isMatch) {
        // ensure inline default is fresh
        SchemaResource shallow = shallowCopyResource(resource);
        rows.add(shallow);
      }
    }
    int from = Math.max(0, page * Math.max(size, 0));
    int to = Math.min(rows.size(), from + Math.max(size, 0));
    if (from >= to) {
      return List.of();
    }
    return new ArrayList<>(rows.subList(from, to));
  }

  private boolean match(SchemaConfig version, String format, Map<String, String> labelFilter) {
    if (version == null) return false;
    if (format != null && !format.equalsIgnoreCase(version.getFormat())) return false;
    if (labelFilter != null && !labelFilter.isEmpty() &&
        (version.getLabels() == null || !labelsMatch(version.getLabels(), labelFilter))) return false;
    return true;
  }

  @Override
  public SchemaResource updateMetadata(@NonNull String schemaId,
                                       String version,
                                       String documentation,
                                       Map<String, String> labels) {
    SchemaResource resource = resourcesBySchemaId.get(schemaId);
    if (resource == null) {
      return null;
    }
    SchemaConfig dv = resource.getVersions().get(version);
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
    dv.setModifiedAt(OffsetDateTime.now());
    // reflect inline
    resource.setDefaultVersion(copyVersion(dv));
    return resource;
  }


  @Override
  public boolean deleteVersion(@NonNull String schemaId, @NonNull String versionId, boolean force) {
    SchemaResource resource = resourcesBySchemaId.get(schemaId);
    if (resource == null || resource.getVersions() == null) {
      return false;
    }

    boolean removedFlag = resource.getVersions().remove(versionId) != null;
    if (!removedFlag) {
      return false;
    }
    if (resource.getDefaultVersion() != null && resource.getDefaultVersion().getVersionId().equals(versionId)) {
      resource.setDefaultVersion(null);
    }

    if (resource.getVersions().isEmpty() && resource.getDefaultVersion() == null) {
      resourcesBySchemaId.remove(schemaId);
    }
    return true;
  }

  @Override
  public boolean deleteSchema(@NonNull String schemaId, boolean force) {
    SchemaResource resource = resourcesBySchemaId.get(schemaId);
    if (resource == null) {
      return false;
    }
    if (!force && resource.getVersions() != null && !resource.getVersions().isEmpty()) {
      return false;
    }
    resourcesBySchemaId.remove(schemaId);
    return true;
  }

  private SchemaConfig prepareVersionForInsert(@NonNull SchemaConfig version) {
    SchemaConfig v = copyVersion(version);
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

  private SchemaConfig resolveDefault(SchemaResource resource) {
    return resource.getDefaultVersion();
  }

  private SchemaConfig copyVersion(SchemaConfig v) {
    return SchemaConfigFactory.getInstance().constructConfig(v);
  }

  private SchemaResource shallowCopyResource(SchemaResource r) {
    SchemaResource c = new SchemaResource();
    c.setSchemaId(r.getSchemaId());
    c.setVersions(r.getVersions());
    // do not copy versions map for search rows
    return c;
  }
}
