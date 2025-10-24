package io.mapsmessaging.schemas.repository.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.schemas.config.SchemaConfigFactory;
import io.mapsmessaging.schemas.model.XRegistrySchemaResource;
import io.mapsmessaging.schemas.model.XRegistrySchemaVersion;
import lombok.NonNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.*;

public class FileSchemaRepository extends SimpleSchemaRepository {

  private static final String DEFAULT_POINTER = "_default";
  private static final String META_JSON = "_meta.json";
  private static final String VERSION_SUFFIX = ".bin";

  private final Logger logger = LoggerFactory.getLogger(FileSchemaRepository.class);
  private final File rootDirectory;
  private final Gson gson;

  public FileSchemaRepository(@NonNull File rootDirectory) throws IOException {
    super();
    this.rootDirectory = rootDirectory;
    this.gson = new GsonBuilder().create();

    if (!rootDirectory.exists() && !rootDirectory.mkdirs()) {
      logger.log(FILE_REPO_ROOT_CREATION_EXCEPTION, rootDirectory.getPath());
      throw new IOException("Unable to create root directory " + rootDirectory.getPath());
    }
    if (!rootDirectory.isDirectory()) {
      logger.log(FILE_REPO_ROOT_NOT_DIRECTORY_EXCEPTION, rootDirectory.getPath());
      throw new IOException("Root directory must be a directory");
    }
    loadAll();
  }

  @Override
  public XRegistrySchemaResource createSchema(@NonNull String schemaId, XRegistrySchemaVersion initialVersion) {
    XRegistrySchemaResource resource = super.createSchema(schemaId, initialVersion);
    File schemaDir = new File(rootDirectory, schemaId);
    if (!schemaDir.exists() && !schemaDir.mkdirs()) {
      logger.log(FILE_REPO_UNABLE_TO_SAVE_EXCEPTION, "Unable to create dir " + schemaDir.getAbsolutePath());
    }
    if (initialVersion != null) {
      writeVersion(schemaDir, resource.getVersionId(), resource.getDefaultVersion());
      writeDefault(schemaDir, resource.getVersionId());
      writeMeta(schemaDir, resource.getMeta());
    } else {
      writeMeta(schemaDir, resource.getMeta());
    }
    return resource;
  }

  @Override
  public XRegistrySchemaVersion addVersion(@NonNull String schemaId, @NonNull XRegistrySchemaVersion version) {
    XRegistrySchemaVersion created = super.addVersion(schemaId, version);
    File schemaDir = new File(rootDirectory, schemaId);
    if (!schemaDir.exists() && !schemaDir.mkdirs()) {
      logger.log(FILE_REPO_UNABLE_TO_SAVE_EXCEPTION, "Unable to create dir " + schemaDir.getAbsolutePath());
      return created;
    }
    writeVersion(schemaDir, created.getVersionId(), created);
    return created;
  }

  @Override
  public XRegistrySchemaResource setDefaultVersion(@NonNull String schemaId, @NonNull String versionId) {
    XRegistrySchemaResource resource = super.setDefaultVersion(schemaId, versionId);
    if (resource == null) {
      return null;
    }
    File schemaDir = new File(rootDirectory, schemaId);
    writeDefault(schemaDir, versionId);
    return resource;
  }

  @Override
  public XRegistrySchemaResource updateMetadata(@NonNull String schemaId,
                                                String documentation,
                                                Map<String, String> labels,
                                                Map<String, Object> meta) {
    XRegistrySchemaResource resource = super.updateMetadata(schemaId, documentation, labels, meta);
    if (resource != null) {
      File schemaDir = new File(rootDirectory, schemaId);
      writeMeta(schemaDir, resource.getMeta());
      if (resource.getDefaultVersion() != null) {
        writeVersion(schemaDir, resource.getDefaultVersion().getVersionId(), resource.getDefaultVersion());
      }
    }
    return resource;
  }

  @Override
  public boolean deleteVersion(@NonNull String schemaId, @NonNull String versionId, boolean force) {
    boolean removed = super.deleteVersion(schemaId, versionId, force);
    if (!removed) {
      return false;
    }
    File schemaDir = new File(rootDirectory, schemaId);
    File versionFile = new File(schemaDir, versionId + VERSION_SUFFIX);
    try {
      Files.deleteIfExists(versionFile.toPath());
    } catch (IOException e) {
      logger.log(FILE_REPO_UNABLE_TO_DELETE_EXCEPTION, e);
    }
    return true;
  }

  @Override
  public boolean deleteSchema(@NonNull String schemaId, boolean force) {
    boolean deleted = super.deleteSchema(schemaId, force);
    if (!deleted) {
      return false;
    }
    File schemaDir = new File(rootDirectory, schemaId);
    deleteDirectory(schemaDir);
    return true;
  }

  private void loadAll() throws IOException {
    File[] schemaDirs = rootDirectory.listFiles(File::isDirectory);
    if (schemaDirs == null) {
      return;
    }
    for (File schemaDir : schemaDirs) {
      String schemaId = schemaDir.getName();

      XRegistrySchemaResource resource = new XRegistrySchemaResource();
      resource.setSchemaId(schemaId);
      resource.setXid(schemaId);
      resource.setVersions(new LinkedHashMap<>());

      Map<String, Object> meta = readMeta(schemaDir);
      if (meta != null && !meta.isEmpty()) {
        resource.setMeta(new LinkedHashMap<>(meta));
      }

      File[] versionFiles = schemaDir.listFiles(pathname -> pathname.isFile() && pathname.getName().endsWith(VERSION_SUFFIX));

      if (versionFiles != null) {
        for (File vf : versionFiles) {
          XRegistrySchemaVersion version = readVersion(vf);
          if (version != null) {
            resource.getVersions().put(version.getVersionId(), version);
          }
        }
      }
      resource.setVersionsCount(resource.getVersions().size());

      String defaultVersionId = readDefault(schemaDir);
      if (defaultVersionId != null && resource.getVersions().containsKey(defaultVersionId)) {
        resource.setVersionId(defaultVersionId);
        resource.setDefaultVersion(copyVersion(resource.getVersions().get(defaultVersionId)));
      }

      resourcesBySchemaId.put(schemaId, resource);
    }
  }

  private void writeVersion(File schemaDir, String versionId, XRegistrySchemaVersion version) {
    if (versionId == null || version == null) {
      return;
    }
    File file = new File(schemaDir, versionId + VERSION_SUFFIX);
    try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
      byte[] payload = version.pack();
      out.write(payload);
      out.flush();
    } catch (IOException e) {
      logger.log(FILE_REPO_UNABLE_TO_SAVE_EXCEPTION, e);
    }
  }

  private XRegistrySchemaVersion readVersion(File versionFile) throws IOException {
    try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(versionFile))) {
      byte[] bytes = in.readAllBytes();
      if (bytes.length == 0) {
        throw new EOFException("Empty version file: " + versionFile.getAbsolutePath());
      }
      XRegistrySchemaVersion version = SchemaConfigFactory.getInstance().constructConfig(bytes);
      if (version.getCreatedAt() == null) {
        version.setCreatedAt(OffsetDateTime.now());
      }
      if (version.getModifiedAt() == null) {
        version.setModifiedAt(version.getCreatedAt());
      }
      return version;
    }
  }

  private void writeDefault(File schemaDir, String versionId) {
    if (versionId == null) {
      return;
    }
    File file = new File(schemaDir, DEFAULT_POINTER);
    try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
      out.write(versionId.getBytes(StandardCharsets.UTF_8));
      out.flush();
    } catch (IOException e) {
      logger.log(FILE_REPO_UNABLE_TO_SAVE_EXCEPTION, e);
    }
  }

  private String readDefault(File schemaDir) throws IOException {
    File file = new File(schemaDir, DEFAULT_POINTER);
    if (!file.exists()) {
      return null;
    }
    byte[] bytes = Files.readAllBytes(file.toPath());
    String s = new String(bytes, StandardCharsets.UTF_8).trim();
    return s.isEmpty() ? null : s;
  }

  private void writeMeta(File schemaDir, Map<String, Object> meta) {
    if (meta == null) {
      return;
    }
    File file = new File(schemaDir, META_JSON);
    try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
      byte[] json = gson.toJson(meta).getBytes(StandardCharsets.UTF_8);
      out.write(json);
      out.flush();
    } catch (IOException e) {
      logger.log(FILE_REPO_UNABLE_TO_SAVE_EXCEPTION, e);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> readMeta(File schemaDir) throws IOException {
    File file = new File(schemaDir, META_JSON);
    if (!file.exists()) {
      return new LinkedHashMap<>();
    }
    String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
    Map<?, ?> parsed = gson.fromJson(json, Map.class);
    Map<String, Object> result = new LinkedHashMap<>();
    if (parsed != null) {
      for (Map.Entry<?, ?> entry : parsed.entrySet()) {
        if (entry.getKey() != null) {
          result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
      }
    }
    return result;
  }

  private void deleteDirectory(File dir) {
    if (!dir.exists()) {
      return;
    }
    File[] files = dir.listFiles();
    if (files != null) {
      for (File f : files) {
        if (f.isDirectory()) {
          deleteDirectory(f);
        } else {
          try {
            Files.deleteIfExists(f.toPath());
          } catch (IOException e) {
            logger.log(FILE_REPO_UNABLE_TO_DELETE_EXCEPTION, e);
          }
        }
      }
    }
    try {
      Files.deleteIfExists(dir.toPath());
    } catch (IOException e) {
      logger.log(FILE_REPO_UNABLE_TO_DELETE_EXCEPTION, e);
    }
  }

  private XRegistrySchemaVersion copyVersion(XRegistrySchemaVersion v) {
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
}
