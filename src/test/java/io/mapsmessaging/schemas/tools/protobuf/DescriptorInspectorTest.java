/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 *
 */

package io.mapsmessaging.schemas.tools.protobuf;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.DescriptorProtos;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DescriptorInspectorTest {

  @TempDir
  Path tempDirectory;

  @Test
  void inspectPrintsFilesMessagesEnumsAndServices() throws Exception {
    DescriptorProtos.FileDescriptorProto file = DescriptorProtos.FileDescriptorProto.newBuilder()
        .setName("sample.proto")
        .addMessageType(DescriptorProtos.DescriptorProto.newBuilder().setName("Message"))
        .addEnumType(DescriptorProtos.EnumDescriptorProto.newBuilder().setName("Mode"))
        .addService(DescriptorProtos.ServiceDescriptorProto.newBuilder().setName("Service"))
        .build();

    DescriptorProtos.FileDescriptorSet set = DescriptorProtos.FileDescriptorSet.newBuilder()
        .addFile(file)
        .build();

    Path descriptor = tempDirectory.resolve("sample.desc");
    Files.write(descriptor, set.toByteArray());

    PrintStream original = System.out;
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try {
      System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
      DescriptorInspector.inspect(descriptor);
    } finally {
      System.setOut(original);
    }

    String text = output.toString(StandardCharsets.UTF_8);
    assertTrue(text.contains("Files in set: 1"));
    assertTrue(text.contains("File: sample.proto"));
    assertTrue(text.contains("Message: Message"));
    assertTrue(text.contains("Enum: Mode"));
    assertTrue(text.contains("Service: Service"));
  }

  @Test
  void inspectPropagatesMissingFileError() {
    assertThrows(IOException.class,
        () -> DescriptorInspector.inspect(tempDirectory.resolve("missing.desc")));
  }
}
