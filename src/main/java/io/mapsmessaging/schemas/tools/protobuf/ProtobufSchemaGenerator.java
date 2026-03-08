/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.mapsmessaging.schemas.tools.protobuf;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.schemas.config.impl.ProtoBufSchemaConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.PROTOBUF_FAILED_TO_DELETE;
import static io.mapsmessaging.schemas.logging.SchemaLogMessages.PROTOBUF_PARSE_EXCEPTION;

public class ProtobufSchemaGenerator {

  private static final Logger logger = LoggerFactory.getLogger(ProtobufSchemaGenerator.class);

  public static List<ProtoBufSchemaConfig> loadSchemas(Path rootPath, Path protocCommandPath) throws IOException {
    Path tempDir = Files.createTempDirectory("proto-desc-" + System.nanoTime());
    try {
      ProtoDescriptorCompiler generator = new ProtoDescriptorCompiler(protocCommandPath);
      try {
        generator.compileAllUnderRoot(rootPath, tempDir);
        return loadDescriptors(tempDir);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted while compiling protobuf", e);
      }
    } finally {
      deleteDirectory(tempDir);
    }
  }


  private static List<ProtoBufSchemaConfig> loadDescriptors(Path outputPath) throws IOException {
    List<ProtoBufSchemaConfig> configs = new ArrayList<>();

    try (Stream<Path> stream = Files.walk(outputPath)) {
      stream
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".desc"))
          .forEach(path -> {
            try {
              byte[] descriptorBytes = Files.readAllBytes(path);
              ProtoBufSchemaConfig config = new ProtoBufSchemaConfig();
              ProtoBufSchemaConfig.ProtobufConfig protobufConfig = new ProtoBufSchemaConfig.ProtobufConfig();
              protobufConfig.setDescriptorValue(descriptorBytes);
              config.setName(path.getFileName().toString());
              config.setUniqueId(UUID.nameUUIDFromBytes(config.getName().getBytes()));
              config.setVersion(1);
              config.setSource(path.toString());
              // use file name (without extension) as a reasonable default
              String fileName = path.getFileName().toString();
              int idx = fileName.lastIndexOf('.');
              protobufConfig.setMessageName(idx > 0 ? fileName.substring(0, idx) : fileName);
              config.setProtobufConfig(protobufConfig);
              configs.add(config);
            } catch (IOException e) {
              logger.log(PROTOBUF_PARSE_EXCEPTION, path.toString(), e);
            }
          });
    }
    return configs;
  }

  private static void deleteDirectory(Path directory) throws IOException {
    if (directory == null || !Files.exists(directory)) {
      return;
    }

    try (Stream<Path> stream = Files.walk(directory)) {
      stream.sorted(Comparator.reverseOrder())
          .forEach(path -> {
            try {
              Files.deleteIfExists(path);
            } catch (IOException e) {
              logger.log(PROTOBUF_FAILED_TO_DELETE, path.toString(), e);
            }
          });
    }
  }

  private ProtobufSchemaGenerator() {
  }

}
