/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 */
package io.mapsmessaging.schemas.config.impl.protobuf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.DescriptorProtos;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DescriptorLoaderCoverageTest {

  private final DescriptorLoader loader = new DescriptorLoader();

  @Test
  void loadsFilesInDependencyOrderRegardlessOfInputOrder() throws Exception {
    DescriptorProtos.FileDescriptorProto dependency =
        DescriptorProtos.FileDescriptorProto.newBuilder()
            .setName("base.proto")
            .setPackage("test")
            .setSyntax("proto3")
            .addMessageType(
                DescriptorProtos.DescriptorProto.newBuilder().setName("Base")
            )
            .build();

    DescriptorProtos.FileDescriptorProto dependent =
        DescriptorProtos.FileDescriptorProto.newBuilder()
            .setName("child.proto")
            .setPackage("test")
            .setSyntax("proto3")
            .addDependency("base.proto")
            .addMessageType(
                DescriptorProtos.DescriptorProto.newBuilder()
                    .setName("Child")
                    .addField(
                        DescriptorProtos.FieldDescriptorProto.newBuilder()
                            .setName("base")
                            .setNumber(1)
                            .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE)
                            .setTypeName(".test.Base")
                    )
            )
            .build();

    byte[] image = DescriptorProtos.FileDescriptorSet.newBuilder()
        .addFile(dependent)
        .addFile(dependency)
        .build()
        .toByteArray();

    Map<String, com.google.protobuf.Descriptors.FileDescriptor> files =
        loader.loadDescFiles(image);

    assertEquals(2, files.size());
    assertNotNull(files.get("base.proto"));
    assertNotNull(files.get("child.proto"));
  }

  @Test
  void unresolvedDependencyFailsExplicitly() {
    DescriptorProtos.FileDescriptorProto broken =
        DescriptorProtos.FileDescriptorProto.newBuilder()
            .setName("broken.proto")
            .setPackage("test")
            .setSyntax("proto3")
            .addDependency("missing.proto")
            .build();

    byte[] image = DescriptorProtos.FileDescriptorSet.newBuilder()
        .addFile(broken)
        .build()
        .toByteArray();

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> loader.loadDescFiles(image));

    assertTrue(exception.getMessage().contains("missing.proto"));
  }

  @Test
  void messageLoaderIncludesNestedMessagesAndLookupHandlesMissingName() throws Exception {
    DescriptorProtos.DescriptorProto nested =
        DescriptorProtos.DescriptorProto.newBuilder().setName("Nested").build();

    DescriptorProtos.DescriptorProto root =
        DescriptorProtos.DescriptorProto.newBuilder()
            .setName("Root")
            .addNestedType(nested)
            .build();

    DescriptorProtos.FileDescriptorProto file =
        DescriptorProtos.FileDescriptorProto.newBuilder()
            .setName("nested.proto")
            .setPackage("test")
            .setSyntax("proto3")
            .addMessageType(root)
            .build();

    byte[] image = DescriptorProtos.FileDescriptorSet.newBuilder()
        .addFile(file)
        .build()
        .toByteArray();

    var messages = loader.loadMessageDescriptors(image);

    assertNotNull(messages.get("test.Root"));
    assertNotNull(messages.get("test.Root.Nested"));
    assertNotNull(loader.findMessageDescriptor(image, "test.Root.Nested"));
    assertNull(loader.findMessageDescriptor(image, "test.Missing"));
  }
}
