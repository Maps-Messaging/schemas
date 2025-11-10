package io.mapsmessaging.schemas.repository.impl;

import com.google.gson.Gson;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.schemas.config.GsonFactory;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.SchemaResource;
import lombok.NonNull;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.*;

public class FileSchemaRepository extends SimpleSchemaRepository {

  private static final String VERSION_SUFFIX = ".schema";

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
  public synchronized SchemaResource createSchema(@NonNull String schemaId, SchemaConfig initialVersion) {
    SchemaResource resource = super.createSchema(schemaId, initialVersion);
    writeVersion(rootDirectory, resource);
    return resource;
  }

  @Override
  public synchronized SchemaResource addVersion(@NonNull String schemaId, @NonNull SchemaConfig version) {
    SchemaResource created = super.addVersion(schemaId, version);
    writeVersion(rootDirectory, created);
    return created;
  }

  @Override
  public synchronized SchemaResource setDefaultVersion(@NonNull String schemaId, @NonNull String versionId) {
    SchemaResource resource = super.setDefaultVersion(schemaId, versionId);
    if (resource == null) {
      return null;
    }
    writeVersion(rootDirectory, resource);
    return resource;
  }

  @Override
  public synchronized SchemaResource updateMetadata(@NonNull String schemaId,
                                       String version,
                                       String documentation,
                                       Map<String, String> labels) {
    SchemaResource resource = super.updateMetadata(schemaId, version, documentation, labels);
    if (resource != null) {
      File schemaDir = new File(rootDirectory, schemaId);
      writeVersion(schemaDir, resource);
    }
    return resource;
  }

  @Override
  public synchronized boolean deleteVersion(@NonNull String schemaId, @NonNull String versionId, boolean force) {
    boolean removed = super.deleteVersion(schemaId, versionId, force);
    if (!removed) {
      return false;
    }
    SchemaResource resource = super.getResource(schemaId);
    if (resource == null || resource.isEmpty()) {
      deleteResource(schemaId);
    }
    return true;
  }

  @Override
  public synchronized boolean deleteResource(String schemaId) {
    if (super.deleteResource(schemaId)) {
      File versionFile = new File(rootDirectory, schemaId + VERSION_SUFFIX);
      try {
        return Files.deleteIfExists(versionFile.toPath());
      } catch (IOException e) {
        logger.log(FILE_REPO_UNABLE_TO_DELETE_EXCEPTION, e);
      }
    }
    return false;
  }


  @Override
  public synchronized List<SchemaConfig> listVersions(@NonNull String schemaId, int page, int size) {
    return super.listVersions(schemaId, page, size);
  }

  @Override
  public synchronized List<SchemaResource> search(String format, Map<String, String> labelFilter, int page, int size) {
    return super.search(format, labelFilter, page, size);
  }

  @Override
  public synchronized List<SchemaResource> getAllSchemas() {
    return super.getAllSchemas();
  }

  private void loadAll() throws IOException {
    File[] schemaDirs = rootDirectory.listFiles(File::isFile);
    if (schemaDirs == null) {
      return;
    }
    for (File schemaDir : schemaDirs) {
      SchemaResource resource1 = readVersion(schemaDir);
      resourcesBySchemaId.put(resource1.getSchemaId(), resource1);
    }
  }

  private void writeVersion(File schemaDir, SchemaResource resource) {
    if (resource == null) {
      return;
    }
    File file = new File(schemaDir, resource.getSchemaId() + VERSION_SUFFIX);
    try {
      Files.write(file.toPath(), gson.toJson(resource).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException e) {
      logger.log(FILE_REPO_UNABLE_TO_SAVE_EXCEPTION, e);
    }
  }

  private SchemaResource readVersion(File versionFile) throws IOException {
    byte[] bytes = Files.readAllBytes(versionFile.toPath());
    if (bytes.length == 0) {
      throw new EOFException("Empty version file: " + versionFile.getAbsolutePath());
    }
    SchemaResource version = gson.fromJson(new String(bytes), SchemaResource.class);
    for (SchemaConfig entry : version.getAll()) {
      if (entry.getCreatedAt() == null) {
        entry.setCreatedAt(OffsetDateTime.now());
      }
      if (entry.getModifiedAt() == null) {
        entry.setModifiedAt(entry.getCreatedAt());
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
