/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 *
 */

package io.mapsmessaging.schemas.tools.protobuf;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.schemas.config.impl.ProtoBufSchemaConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProtobufSchemaGeneratorTest {

  @TempDir
  Path tempDirectory;

  @Test
  void privateTempDirectoryIsCreatedUnderUserHome() throws Exception {
    Path directory = ProtobufSchemaGenerator.createPrivateTempDirectory();
    try {
      Path userHome = Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
      assertTrue(directory.startsWith(userHome));
      assertTrue(Files.isDirectory(directory));
    } finally {
      Files.deleteIfExists(directory);
    }
  }

  @Test
  void loadSchemasBuildsConfigFromGeneratedDescriptor() throws Exception {
    Path root = Files.createDirectories(tempDirectory.resolve("schemas"));
    Files.writeString(
        root.resolve("sample.proto"),
        "syntax = \"proto3\";\nmessage Sample { string value = 1; }\n"
    );

    Path protoc = createFakeProtoc(tempDirectory.resolve("protoc-bin"));

    List<ProtoBufSchemaConfig> configs = ProtobufSchemaGenerator.loadSchemas(root, protoc);

    assertEquals(1, configs.size());

    ProtoBufSchemaConfig config = configs.getFirst();
    assertEquals("all.desc", config.getName());
    assertEquals("1", config.getVersion());
    assertEquals("all", config.getProtobufConfig().getMessageName());
    assertArrayEquals(
        "fake descriptor".getBytes(StandardCharsets.UTF_8),
        config.getProtobufConfig().getDescriptorValue()
    );
    assertTrue(config.getSource().endsWith("all.desc"));
    assertEquals(
        java.util.UUID.nameUUIDFromBytes("all.desc".getBytes()).toString(),
        config.getUniqueId()
    );
  }

  @Test
  void loadSchemasRejectsRootWithoutProtoFiles() throws Exception {
    Path root = Files.createDirectories(tempDirectory.resolve("empty"));
    Path protoc = createFakeProtoc(tempDirectory.resolve("empty-protoc-bin"));

    IOException exception = assertThrows(
        IOException.class,
        () -> ProtobufSchemaGenerator.loadSchemas(root, protoc)
    );

    assertTrue(exception.getMessage().contains("No .proto files"));
  }

  private Path createFakeProtoc(Path directory) throws IOException {
    Files.createDirectories(directory);
    return isWindows() ? createWindowsFakeProtoc(directory) : createUnixFakeProtoc(directory);
  }

  private Path createUnixFakeProtoc(Path directory) throws IOException {
    Path script = directory.resolve("protoc");
    Files.writeString(
        script,
        "#!/bin/sh\n"
            + "OUTFILE=\"\"\n"
            + "while [ \"$#\" -gt 0 ]; do\n"
            + "  case \"$1\" in\n"
            + "    --descriptor_set_out=*) OUTFILE=$(printf '%s' \"$1\" | sed 's/^--descriptor_set_out=//') ;;\n"
            + "  esac\n"
            + "  shift\n"
            + "done\n"
            + "if [ -z \"$OUTFILE\" ]; then exit 1; fi\n"
            + "printf 'fake descriptor' > \"$OUTFILE\"\n"
            + "exit 0\n"
    );
    try {
      Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwxr-xr-x"));
    } catch (UnsupportedOperationException ignored) {
      if (!script.toFile().setExecutable(true)) {
        throw new IOException("Failed to mark fake protoc executable");
      }
    }
    return script;
  }

  private Path createWindowsFakeProtoc(Path directory) throws IOException {
    Path script = directory.resolve("protoc.cmd");
    Files.writeString(
        script,
        "@echo off\r\n"
            + "setlocal EnableDelayedExpansion\r\n"
            + "set \"OUTFILE=\"\r\n"
            + ":loop\r\n"
            + "if \"%~1\"==\"\" goto done\r\n"
            + "set \"ARG=%~1\"\r\n"
            + "if /I \"!ARG:~0,21!\"==\"--descriptor_set_out=\" set \"OUTFILE=!ARG:~21!\"\r\n"
            + "shift\r\n"
            + "goto loop\r\n"
            + ":done\r\n"
            + "if \"!OUTFILE!\"==\"\" exit /b 1\r\n"
            + "<nul set /p =fake descriptor>\"!OUTFILE!\"\r\n"
            + "exit /b 0\r\n"
    );
    return script;
  }

  private boolean isWindows() {
    return System.getProperty("os.name", "").toLowerCase().contains("win");
  }
}
