/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 *
 */

package io.mapsmessaging.schemas.tools.protobuf;

import io.mapsmessaging.schemas.config.impl.ProtoBufSchemaConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

    ProtoBufSchemaConfig config = configs.get(0);
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
    Files.writeString(directory.resolve("fixture.desc"), "fake descriptor", StandardCharsets.UTF_8);
    Files.writeString(
        script,
        "@echo off\r\n"
            + "setlocal\r\n"
            + "rem Compiler command: -I root --include_imports --descriptor_set_out=output file.proto\r\n"
            + "set \"ARG=%~4\"\r\n"
            + "if /I not \"%ARG:~0,21%\"==\"--descriptor_set_out=\" (\r\n"
            + "  echo Unexpected descriptor argument: %ARG% 1>&2\r\n"
            + "  exit /b 1\r\n"
            + ")\r\n"
            + "set \"OUTFILE=%ARG:~21%\"\r\n"
            + "if \"%OUTFILE%\"==\"\" (\r\n"
            + "  echo Missing --descriptor_set_out argument 1>&2\r\n"
            + "  exit /b 1\r\n"
            + ")\r\n"
            + "copy /b /y \"%~dp0fixture.desc\" \"%OUTFILE%\" >nul\r\n"
            + "if errorlevel 1 (\r\n"
            + "  echo Failed to create descriptor: %OUTFILE% 1>&2\r\n"
            + "  exit /b 1\r\n"
            + ")\r\n"
            + "exit /b 0\r\n"
    );
    return script;
  }

  private boolean isWindows() {
    return System.getProperty("os.name", "").toLowerCase().contains("win");
  }
}
