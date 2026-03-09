/*
 *
 *  Copyright [ 2026 - 2026 ] MapsMessaging B.V.
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

import lombok.Getter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;

public class ProtoDescriptorCompiler {

  private final Path configuredProtocPath;

  @Getter
  private Path resolvedProtocPath;

  public ProtoDescriptorCompiler() {
    this(null);
  }

  public ProtoDescriptorCompiler(Path configuredProtocPath) {
    this.configuredProtocPath = configuredProtocPath;
  }

  public boolean isAvailable() {
    try {
      resolveProtocPath();
      return true;
    } catch (IOException ignored) {
      return false;
    }
  }

  public Path getProtocPath() throws IOException {
    return resolveProtocPath();
  }

  public String getProtocVersion() throws IOException, InterruptedException {
    Path protocPath = resolveProtocPath();

    List<String> command = new ArrayList<>();
    command.add(protocPath.toString());
    command.add("--version");

    ProcessResult processResult = execute(command, null);
    if (processResult.exitCode != 0) {
      throw new IOException("Failed to execute protoc --version\n" + processResult.output);
    }
    return processResult.output.trim();
  }

  public Path compile(Path rootPath, Path protoRootPath, Path descriptorOutputPath) throws IOException, InterruptedException {
    return compile(rootPath, protoRootPath, descriptorOutputPath, List.of());
  }

  public Path compile(
      Path rootPath,
      Path protoRootPath,
      Path descriptorOutputPath,
      List<Path> additionalIncludePaths
  ) throws IOException, InterruptedException {
    Path normalizedRootPath = validateRootPath(rootPath);
    Path normalizedProtoRootPath = validateProtoRootPath(protoRootPath);
    validateProtoRootUnderRoot(normalizedRootPath, normalizedProtoRootPath);
    validateDescriptorOutputPath(descriptorOutputPath);

    List<Path> protoFiles = findProtoFilesInSingleDirectory(normalizedProtoRootPath);
    if (protoFiles.isEmpty()) {
      throw new IOException("No .proto files found directly under " + normalizedProtoRootPath);
    }

    Path normalizedDescriptorOutputPath = descriptorOutputPath.toAbsolutePath().normalize();
    Path parentPath = normalizedDescriptorOutputPath.getParent();
    if (parentPath != null) {
      Files.createDirectories(parentPath);
    }

    List<String> command = buildCompileCommand(
        normalizedRootPath,
        normalizedProtoRootPath,
        normalizedDescriptorOutputPath,
        protoFiles,
        additionalIncludePaths
    );

    ProcessResult processResult = execute(command, normalizedRootPath);
    if (processResult.exitCode != 0) {
      throw new IOException("protoc failed with exit code " + processResult.exitCode + "\n" + processResult.output);
    }

    if (!Files.exists(normalizedDescriptorOutputPath) || Files.size(normalizedDescriptorOutputPath) == 0) {
      throw new IOException("Descriptor output file was not created: " + normalizedDescriptorOutputPath);
    }

    return normalizedDescriptorOutputPath;
  }

  public Path compileIfRequired(
      Path rootPath,
      Path protoRootPath,
      Path descriptorOutputPath,
      List<Path> additionalIncludePaths
  ) throws IOException, InterruptedException {
    Path normalizedRootPath = validateRootPath(rootPath);
    Path normalizedProtoRootPath = validateProtoRootPath(protoRootPath);
    validateProtoRootUnderRoot(normalizedRootPath, normalizedProtoRootPath);
    validateDescriptorOutputPath(descriptorOutputPath);

    List<Path> protoFiles = findProtoFilesInSingleDirectory(normalizedProtoRootPath);
    if (protoFiles.isEmpty()) {
      throw new IOException("No .proto files found directly under " + normalizedProtoRootPath);
    }

    Path normalizedDescriptorOutputPath = descriptorOutputPath.toAbsolutePath().normalize();
    if (!needsRebuild(protoFiles, normalizedDescriptorOutputPath)) {
      return normalizedDescriptorOutputPath;
    }

    return compile(normalizedRootPath, normalizedProtoRootPath, normalizedDescriptorOutputPath, additionalIncludePaths);
  }

  public List<Path> compileAllUnderRoot(Path rootPath, Path outputDirectoryPath) throws IOException, InterruptedException {
    return compileAllUnderRoot(rootPath, outputDirectoryPath, List.of());
  }

  public List<Path> compileAllUnderRoot(
      Path rootPath,
      Path outputDirectoryPath,
      List<Path> additionalIncludePaths
  ) throws IOException, InterruptedException {
    Path normalizedRootPath = validateRootPath(rootPath);
    Objects.requireNonNull(outputDirectoryPath, "outputDirectoryPath must not be null");

    Path normalizedOutputDirectoryPath = outputDirectoryPath.toAbsolutePath().normalize();
    Files.createDirectories(normalizedOutputDirectoryPath);

    List<Path> schemaDirectories = findProtoDirectories(normalizedRootPath);
    if (schemaDirectories.isEmpty()) {
      throw new IOException("No directories containing .proto files found under " + normalizedRootPath);
    }

    List<Path> descriptorPaths = new ArrayList<>();
    for (Path schemaDirectory : schemaDirectories) {
      Path relativeDirectory = normalizedRootPath.relativize(schemaDirectory);

      String descriptorFileName;
      if (relativeDirectory.getNameCount() == 0) {
        descriptorFileName = "root.desc";
      } else {
        descriptorFileName = relativeDirectory.toString().replace('\\', '_').replace('/', '_') + ".desc";
      }

      Path descriptorOutputPath = normalizedOutputDirectoryPath.resolve(descriptorFileName);
      Path compiledPath = compileIfRequired(normalizedRootPath, schemaDirectory, descriptorOutputPath, additionalIncludePaths);
      descriptorPaths.add(compiledPath);
    }

    descriptorPaths.sort(Comparator.comparing(path -> path.toAbsolutePath().toString()));
    return descriptorPaths;
  }

  public List<Path> findProtoDirectories(Path rootPath) throws IOException {
    Path normalizedRootPath = validateRootPath(rootPath);

    Set<Path> directories = new LinkedHashSet<>();

    Files.walkFileTree(normalizedRootPath, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
        if (attributes.isRegularFile() && file.getFileName().toString().endsWith(".proto")) {
          directories.add(file.getParent().toAbsolutePath().normalize());
        }
        return FileVisitResult.CONTINUE;
      }
    });

    List<Path> results = new ArrayList<>(directories);
    results.sort(Comparator.comparing(path -> path.toAbsolutePath().toString()));
    return results;
  }

  private List<String> buildCompileCommand(
      Path rootPath,
      Path protoRootPath,
      Path descriptorOutputPath,
      List<Path> protoFiles,
      List<Path> additionalIncludePaths
  ) throws IOException {
    Path protocPath = resolveProtocPath();

    Path normalizedRootPath = rootPath.toAbsolutePath().normalize();
    Path normalizedProtoRootPath = protoRootPath.toAbsolutePath().normalize();

    List<String> command = new ArrayList<>();
    command.add(protocPath.toString());

    command.add("-I");
    command.add(normalizedRootPath.toString());

    if (!normalizedProtoRootPath.equals(normalizedRootPath)) {
      command.add("-I");
      command.add(normalizedProtoRootPath.toString());
    }

    if (additionalIncludePaths != null) {
      for (Path includePath : additionalIncludePaths) {
        if (includePath != null) {
          command.add("-I");
          command.add(includePath.toAbsolutePath().normalize().toString());
        }
      }
    }

    command.add("--include_imports");
    command.add("--descriptor_set_out=" + descriptorOutputPath.toAbsolutePath().normalize());

    for (Path protoFile : protoFiles) {
      Path normalizedProtoFile = protoFile.toAbsolutePath().normalize();
      if (!normalizedProtoFile.startsWith(normalizedRootPath)) {
        throw new IOException("Proto file is outside root path: " + normalizedProtoFile);
      }

      String relativeProtoPath = normalizedRootPath.relativize(normalizedProtoFile)
          .toString()
          .replace('\\', '/');

      command.add(relativeProtoPath);
    }

    return command;
  }

  private ProcessResult execute(List<String> command, Path workingDirectory) throws IOException, InterruptedException {
    ProcessBuilder processBuilder = new ProcessBuilder(command);
    processBuilder.redirectErrorStream(true);

    if (workingDirectory != null) {
      processBuilder.directory(workingDirectory.toFile());
    }

    Process process = processBuilder.start();
    String output = readProcessOutput(process);
    int exitCode = process.waitFor();

    return new ProcessResult(exitCode, output);
  }

  private String readProcessOutput(Process process) throws IOException {
    StringBuilder stringBuilder = new StringBuilder();

    try (BufferedReader bufferedReader = new BufferedReader(
        new InputStreamReader(process.getInputStream())
    )) {
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        stringBuilder.append(line).append(System.lineSeparator());
      }
    }

    return stringBuilder.toString();
  }

  private List<Path> findProtoFilesInSingleDirectory(Path protoDirectoryPath) throws IOException {
    List<Path> protoFiles = new ArrayList<>();

    try (var stream = Files.list(protoDirectoryPath)) {
      stream.filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".proto"))
          .map(path -> path.toAbsolutePath().normalize())
          .sorted(Comparator.comparing(path -> path.toAbsolutePath().toString()))
          .forEach(protoFiles::add);
    }

    return protoFiles;
  }

  private boolean needsRebuild(List<Path> protoFiles, Path descriptorOutputPath) throws IOException {
    if (!Files.exists(descriptorOutputPath)) {
      return true;
    }

    Instant descriptorModifiedTime = Files.getLastModifiedTime(descriptorOutputPath).toInstant();
    for (Path protoFile : protoFiles) {
      Instant protoModifiedTime = Files.getLastModifiedTime(protoFile).toInstant();
      if (protoModifiedTime.isAfter(descriptorModifiedTime)) {
        return true;
      }
    }
    return false;
  }

  private Path resolveProtocPath() throws IOException {
    if (resolvedProtocPath != null) {
      return resolvedProtocPath;
    }

    if (configuredProtocPath != null) {
      Path absoluteConfiguredPath = configuredProtocPath.toAbsolutePath().normalize();
      validateExecutable(absoluteConfiguredPath, "Configured protoc path");
      resolvedProtocPath = absoluteConfiguredPath;
      return resolvedProtocPath;
    }

    Path pathResolved = resolveFromPath();
    if (pathResolved != null) {
      resolvedProtocPath = pathResolved;
      return resolvedProtocPath;
    }

    throw new IOException(
        "Unable to locate protoc. Configure an explicit path or ensure protoc is available on PATH."
    );
  }

  private Path resolveFromPath() {
    String pathEnvironment = System.getenv("PATH");
    if (pathEnvironment == null || pathEnvironment.isBlank()) {
      return null;
    }

    String executableName = isWindows() ? "protoc.exe" : "protoc";

    for (String pathEntry : pathEnvironment.split(File.pathSeparator)) {
      if (pathEntry.isBlank()) {
        continue;
      }

      Path candidatePath = Paths.get(pathEntry, executableName).toAbsolutePath().normalize();
      if (Files.exists(candidatePath) && Files.isRegularFile(candidatePath) && Files.isExecutable(candidatePath)) {
        return candidatePath;
      }
    }

    return null;
  }

  private Path validateRootPath(Path rootPath) throws IOException {
    Objects.requireNonNull(rootPath, "rootPath must not be null");

    Path normalizedRootPath = rootPath.toAbsolutePath().normalize();
    if (!Files.exists(normalizedRootPath)) {
      throw new IOException("Root path does not exist: " + normalizedRootPath);
    }
    if (!Files.isDirectory(normalizedRootPath)) {
      throw new IOException("Root path is not a directory: " + normalizedRootPath);
    }

    return normalizedRootPath;
  }

  private Path validateProtoRootPath(Path protoRootPath) throws IOException {
    Objects.requireNonNull(protoRootPath, "protoRootPath must not be null");

    Path normalizedProtoRootPath = protoRootPath.toAbsolutePath().normalize();
    if (!Files.exists(normalizedProtoRootPath)) {
      throw new IOException("Proto root path does not exist: " + normalizedProtoRootPath);
    }
    if (!Files.isDirectory(normalizedProtoRootPath)) {
      throw new IOException("Proto root path is not a directory: " + normalizedProtoRootPath);
    }

    return normalizedProtoRootPath;
  }

  private void validateProtoRootUnderRoot(Path rootPath, Path protoRootPath) throws IOException {
    if (!protoRootPath.startsWith(rootPath)) {
      throw new IOException("Proto root path must be under root path: protoRootPath=" + protoRootPath + ", rootPath=" + rootPath);
    }
  }

  private void validateDescriptorOutputPath(Path descriptorOutputPath) {
    Objects.requireNonNull(descriptorOutputPath, "descriptorOutputPath must not be null");
  }

  private void validateExecutable(Path executablePath, String description) throws IOException {
    if (!Files.exists(executablePath)) {
      throw new IOException(description + " does not exist: " + executablePath);
    }
    if (!Files.isRegularFile(executablePath)) {
      throw new IOException(description + " is not a file: " + executablePath);
    }
    if (!Files.isExecutable(executablePath)) {
      throw new IOException(description + " is not executable: " + executablePath);
    }
  }

  private boolean isWindows() {
    String operatingSystemName = System.getProperty("os.name", "");
    return operatingSystemName.toLowerCase().contains("win");
  }

  private static final class ProcessResult {

    private final int exitCode;
    private final String output;

    private ProcessResult(int exitCode, String output) {
      this.exitCode = exitCode;
      this.output = output;
    }
  }
}