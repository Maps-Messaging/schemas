/*
 *
 *     Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package io.mapsmessaging.schemas.repository.impl;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.FILE_REPO_ROOT_CREATION_EXCEPTION;
import static io.mapsmessaging.schemas.logging.SchemaLogMessages.FILE_REPO_ROOT_NOT_DIRECTORY_EXCEPTION;
import static io.mapsmessaging.schemas.logging.SchemaLogMessages.FILE_REPO_UNABLE_TO_DELETE_EXCEPTION;
import static io.mapsmessaging.schemas.logging.SchemaLogMessages.FILE_REPO_UNABLE_TO_SAVE_EXCEPTION;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.SchemaConfigFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;

public class FileSchemaRepository extends SimpleSchemaRepository{

  private final Logger logger = LoggerFactory.getLogger(FileSchemaRepository.class);

  private final File rootDirectory;

  public FileSchemaRepository(@NonNull File rootDirectory) throws IOException {
    this.rootDirectory = rootDirectory;
    if (!rootDirectory.exists() && !rootDirectory.mkdirs()) {
      logger.log(FILE_REPO_ROOT_CREATION_EXCEPTION, rootDirectory.getPath());
      throw new IOException("Unable to create root directory " + rootDirectory.getPath());
    }
    if (!rootDirectory.isDirectory()) {
      logger.log(FILE_REPO_ROOT_NOT_DIRECTORY_EXCEPTION, rootDirectory.getPath());
      throw new IOException("Root directory must be a directory");
    }
    loadData();
  }

  private void loadData() throws IOException {
    File[] children = rootDirectory.listFiles();
    if(children != null) {
      for (File child : children) {
        try (FileInputStream fileInputStream = new FileInputStream(child)){
          int len = fileInputStream.read() & 0xff;
          len = len | (fileInputStream.read() & 0xff) << 8;
          byte[] contextBytes = new byte[len];
          fileInputStream.read(contextBytes);
          String context = new String(contextBytes);
          int remaining = (int)(child.length() - len);
          byte[] schemaBytes = new byte[remaining];
          fileInputStream.read(schemaBytes);
          SchemaConfig schemaConfig = SchemaConfigFactory.getInstance().constructConfig(schemaBytes);
          addSchema(context, schemaConfig);
        }
      }
    }
  }

  @Override
  public SchemaConfig addSchema(@NonNull String context, @NonNull SchemaConfig config) {
    File schemafile = new File(rootDirectory, config.getUniqueId());
    try (FileOutputStream fileOutputStream = new FileOutputStream(schemafile)) {
      byte[] contextBytes = context.getBytes();
      int len = contextBytes.length;
      fileOutputStream.write(((byte) len & 0xff));
      fileOutputStream.write(((byte) (len >> 8) & 0xff));
      fileOutputStream.write(contextBytes);
      fileOutputStream.write(config.pack().getBytes());
      fileOutputStream.flush();
    }
    catch (IOException ex){
      logger.log(FILE_REPO_UNABLE_TO_SAVE_EXCEPTION, ex);
    }
    return super.addSchema(context, config);
  }

  @Override
  public void removeSchema(@NonNull String uuid) {
    super.removeSchema(uuid);
    try {
      Files.delete(new File(rootDirectory, uuid).toPath());
    } catch (IOException e) {
      logger.log(FILE_REPO_UNABLE_TO_DELETE_EXCEPTION, e);
    }
  }

  @Override
  public void removeAllSchemas() {
    List<String> uniqueIds = new ArrayList<>(super.mapByUUID.keySet());
    for(String uniqueId:uniqueIds){
      removeSchema(uniqueId);
    }
  }
}
