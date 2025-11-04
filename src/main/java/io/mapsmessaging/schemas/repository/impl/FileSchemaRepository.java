package io.mapsmessaging.schemas.repository.impl;

import com.google.gson.Gson;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.schemas.config.GsonFactory;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.model.SchemaResource;
import lombok.NonNull;

import java.io.*;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.*;

public class FileSchemaRepository extends SimpleSchemaRepository {

  private static final String VERSION_SUFFIX = ".schema_resource";

  private final Logger logger = LoggerFactory.getLogger(FileSchemaRepository.class);
  private final File rootDirectory;
  private final Gson gson;

  public FileSchemaRepository(@NonNull File rootDirectory) throws IOException {
    super();
    this.rootDirectory = rootDirectory;
    this.gson = GsonFactory.buildGson();

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
  public SchemaResource createSchema(@NonNull String schemaId, SchemaConfig initialVersion) {
    SchemaResource resource = super.createSchema(schemaId, initialVersion);
    File schemaDir = new File(rootDirectory, schemaId);
    if (!schemaDir.exists() && !schemaDir.mkdirs()) {
      logger.log(FILE_REPO_UNABLE_TO_SAVE_EXCEPTION, "Unable to create dir " + schemaDir.getAbsolutePath());
    }
    writeVersion(schemaDir, resource.getVersionId(), resource);
    return resource;
  }

  @Override
  public SchemaResource addVersion(@NonNull String schemaId, @NonNull SchemaConfig version) {
    SchemaResource created = super.addVersion(schemaId, version);
    File schemaDir = new File(rootDirectory, schemaId);
    if (!schemaDir.exists() && !schemaDir.mkdirs()) {
      logger.log(FILE_REPO_UNABLE_TO_SAVE_EXCEPTION, "Unable to create dir " + schemaDir.getAbsolutePath());
      return created;
    }
    writeVersion(schemaDir, created.getVersionId(), created);
    return created;
  }

  @Override
  public SchemaResource setDefaultVersion(@NonNull String schemaId, @NonNull String versionId) {
    SchemaResource resource = super.setDefaultVersion(schemaId, versionId);
    if (resource == null) {
      return null;
    }
    writeVersion(rootDirectory, resource.getVersionId(), resource);
    return resource;
  }

  @Override
  public SchemaResource updateMetadata(@NonNull String schemaId,
                                       String version,
                                       String documentation,
                                       Map<String, String> labels,
                                       Map<String, Object> meta) {
    SchemaResource resource = super.updateMetadata(schemaId, version, documentation, labels, meta);
    if (resource != null) {
      File schemaDir = new File(rootDirectory, schemaId);
      writeVersion(schemaDir, resource.getVersionId(), resource);
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

      SchemaResource resource = new SchemaResource();
      resource.setSchemaId(schemaId);
      resource.setXid(schemaId);
      resource.setVersions(new LinkedHashMap<>());

      File[] versionFiles = schemaDir.listFiles(pathname -> pathname.isFile() && pathname.getName().endsWith(VERSION_SUFFIX));

      if (versionFiles != null) {
        for (File vf : versionFiles) {
          SchemaResource resource1 = readVersion(vf);
          resourcesBySchemaId.put(resource1.getSchemaId(), resource1);
        }
      }
      resource.setVersionsCount(resource.getVersions().size());
    }
  }

  private void writeVersion(File schemaDir, String versionId, SchemaResource resource) {
    if (versionId == null || resource == null) {
      return;
    }
    File file = new File(schemaDir, versionId + VERSION_SUFFIX);
    try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
      String str = gson.toJson(resource);
      out.write(str.getBytes());
      out.flush();
    } catch (Exception e) {
      e.printStackTrace();
      logger.log(FILE_REPO_UNABLE_TO_SAVE_EXCEPTION, e);
    }
  }

  private SchemaResource readVersion(File versionFile) throws IOException {
    try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(versionFile))) {
      byte[] bytes = in.readAllBytes();
      if (bytes.length == 0) {
        throw new EOFException("Empty version file: " + versionFile.getAbsolutePath());
      }
      SchemaResource version = gson.fromJson(new String(bytes), SchemaResource.class);
      for (Map.Entry<String, SchemaConfig> entry : version.getVersions().entrySet()) {
        if (entry.getValue().getCreatedAt() == null) {
          entry.getValue().setCreatedAt(OffsetDateTime.now());
        }
        if (entry.getValue().getModifiedAt() == null) {
          entry.getValue().setModifiedAt(entry.getValue().getCreatedAt());
        }
      }
      if (version.getDefaultVersion() != null) {
        if (version.getDefaultVersion().getCreatedAt() == null) {
          version.getDefaultVersion().setCreatedAt(OffsetDateTime.now());
        }
        if (version.getDefaultVersion().getModifiedAt() == null) {
          version.getDefaultVersion().setModifiedAt(version.getDefaultVersion().getCreatedAt());
        }
      }
      return version;
    }
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
}
