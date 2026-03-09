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

/*
 *
 *     Copyright [ 2020 - 2026 ] [Matthew Buckton]
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

package io.mapsmessaging.schemas.tools.protobuf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtoDescriptorCompilerTest {

  @TempDir
  Path tempDirectory;

  @Test
  void getProtocVersionReturnsConfiguredVersion() throws Exception {
    Path scriptDirectory = Files.createDirectories(tempDirectory.resolve("script"));
    Path logFile = scriptDirectory.resolve("protoc.log");
    Path fakeProtoc = createFakeProtoc(scriptDirectory, logFile);

    ProtoDescriptorCompiler compiler = new ProtoDescriptorCompiler(fakeProtoc);

    String version = compiler.getProtocVersion();

    assertEquals("libprotoc 99.0", version);
  }

  @Test
  void compileAllUnderRootCreatesDescriptorPerProtoDirectory() throws Exception {
    Path rootPath = Files.createDirectories(tempDirectory.resolve("schemas"));
    createProtoFile(rootPath.resolve("base/authority.proto"), "Authority");
    createProtoFile(rootPath.resolve("base/node/general.proto"), "General");
    createProtoFile(rootPath.resolve("base/task/task.proto"), "Task");
    createProtoFile(rootPath.resolve("catl/hibw/core/core.proto"), "Core");
    createProtoFile(rootPath.resolve("catl/hibw/messages/message.proto"), "Message");

    Path outputDirectoryPath = Files.createDirectories(tempDirectory.resolve("out"));
    Path scriptDirectory = Files.createDirectories(tempDirectory.resolve("script"));
    Path logFile = scriptDirectory.resolve("protoc.log");
    Path fakeProtoc = createFakeProtoc(scriptDirectory, logFile);

    ProtoDescriptorCompiler compiler = new ProtoDescriptorCompiler(fakeProtoc);

    List<Path> compiled = compiler.compileAllUnderRoot(rootPath, outputDirectoryPath);

    assertEquals(5, compiled.size());
    assertTrue(Files.exists(outputDirectoryPath.resolve("base.desc")));
    assertTrue(Files.exists(outputDirectoryPath.resolve("base_node.desc")));
    assertTrue(Files.exists(outputDirectoryPath.resolve("base_task.desc")));
    assertTrue(Files.exists(outputDirectoryPath.resolve("catl_hibw_core.desc")));
    assertTrue(Files.exists(outputDirectoryPath.resolve("catl_hibw_messages.desc")));

    for (Path path : compiled) {
      assertTrue(Files.exists(path));
      assertTrue(Files.size(path) > 0);
    }
  }

  @Test
  void compileIfRequiredSkipsRebuildWhenDescriptorIsNewer() throws Exception {
    Path rootPath = Files.createDirectories(tempDirectory.resolve("schemas"));
    Path protoDirectoryPath = Files.createDirectories(rootPath.resolve("base"));
    createProtoFile(protoDirectoryPath.resolve("authority.proto"), "Authority");

    Path outputDirectoryPath = Files.createDirectories(tempDirectory.resolve("out"));
    Path descriptorOutputPath = outputDirectoryPath.resolve("base.desc");

    Path scriptDirectory = Files.createDirectories(tempDirectory.resolve("script"));
    Path logFile = scriptDirectory.resolve("protoc.log");
    Path fakeProtoc = createFakeProtoc(scriptDirectory, logFile);

    ProtoDescriptorCompiler compiler = new ProtoDescriptorCompiler(fakeProtoc);

    Path firstCompile = compiler.compile(rootPath, protoDirectoryPath, descriptorOutputPath);
    long initialLogLineCount = countLines(logFile);

    Files.setLastModifiedTime(firstCompile, FileTime.from(Instant.now().plusSeconds(60)));

    Path secondCompile = compiler.compileIfRequired(rootPath, protoDirectoryPath, descriptorOutputPath, List.of());
    long finalLogLineCount = countLines(logFile);

    assertEquals(firstCompile, secondCompile);
    assertEquals(initialLogLineCount, finalLogLineCount);
  }

  private void createProtoFile(Path path, String messageName) throws IOException {
    Files.createDirectories(path.getParent());
    Files.writeString(
        path,
        "syntax = \"proto3\";\n" +
            "package test;\n" +
            "message " + messageName + " {\n" +
            "  string value = 1;\n" +
            "}\n"
    );
  }

  private long countLines(Path path) throws IOException {
    if (!Files.exists(path)) {
      return 0;
    }
    try (var stream = Files.lines(path)) {
      return stream.count();
    }
  }

  private Path createFakeProtoc(Path scriptDirectory, Path logFile) throws IOException {
    Path scriptPath = scriptDirectory.resolve("protoc.cmd");
    String script =
        "@echo off\r\n" +
            "setlocal EnableDelayedExpansion\r\n" +
            "set \"LOGFILE=" + logFile.toString() + "\"\r\n" +
            "if \"%~1\"==\"--version\" (\r\n" +
            "  echo libprotoc 99.0\r\n" +
            "  exit /b 0\r\n" +
            ")\r\n" +
            "set \"OUTFILE=\"\r\n" +
            ":loop\r\n" +
            "if \"%~1\"==\"\" goto done\r\n" +
            "echo %~1>>\"%LOGFILE%\"\r\n" +
            "if /I \"%~1\"==\"--descriptor_set_out\" (\r\n" +
            "  set \"OUTFILE=%~2\"\r\n" +
            "  echo OUTFILE=!OUTFILE!>>\"%LOGFILE%\"\r\n" +
            "  shift\r\n" +
            "  shift\r\n" +
            "  goto loop\r\n" +
            ")\r\n" +
            "shift\r\n" +
            "goto loop\r\n" +
            ":done\r\n" +
            "if \"!OUTFILE!\"==\"\" exit /b 1\r\n" +
            "echo fake descriptor>\"!OUTFILE!\"\r\n" +
            "if not exist \"!OUTFILE!\" exit /b 2\r\n" +
            "exit /b 0\r\n";
    Files.writeString(scriptPath, script);
    return scriptPath;
  }

  private boolean isWindows() {
    String operatingSystemName = System.getProperty("os.name", "");
    return operatingSystemName.toLowerCase().contains("win");
  }

  private String escapeForShell(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private String quoteForBatch(String value) {
    return "\"" + value + "\"";
  }
}