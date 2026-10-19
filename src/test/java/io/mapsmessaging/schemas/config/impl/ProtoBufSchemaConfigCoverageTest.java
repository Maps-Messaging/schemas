/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 *
 */

package io.mapsmessaging.schemas.config.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.DescriptorProtos;
import io.mapsmessaging.schemas.config.SchemaConfig;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProtoBufSchemaConfigCoverageTest {

  @Test
  void configRoundTripAndBundleDetectionCoverEmptyAndNamedCases() {
    ProtoBufSchemaConfig config = new ProtoBufSchemaConfig();

    ProtoBufSchemaConfig.ProtobufConfig empty = config.getProtobufConfig();
    assertEquals(null, empty.getMessageName());
    assertEquals(null, empty.getDescriptorValue());
    assertFalse(config.isBundle());

    ProtoBufSchemaConfig.ProtobufConfig named = new ProtoBufSchemaConfig.ProtobufConfig();
    named.setMessageName("Message");
    named.setDescriptorValue(new byte[]{1, 2, 3});
    config.setProtobufConfig(named);

    assertEquals("Message", config.getProtobufConfig().getMessageName());
    assertArrayEquals(new byte[]{1, 2, 3}, config.getProtobufConfig().getDescriptorValue());
    assertFalse(config.isBundle());

    named.setMessageName("all");
    config.setProtobufConfig(named);
    assertTrue(config.isBundle());

    config.setProtobufConfig(null);
    assertFalse(config.isBundle());
  }

  @Test
  void packRejectsMissingDescriptorAndAcceptsValidNamedConfig() throws Exception {
    ProtoBufSchemaConfig config = new ProtoBufSchemaConfig();

    assertThrows(IOException.class, config::pack);

    ProtoBufSchemaConfig.ProtobufConfig protobuf = new ProtoBufSchemaConfig.ProtobufConfig();
    protobuf.setMessageName("Message");
    protobuf.setDescriptorValue(descriptorSet());
    config.setProtobufConfig(protobuf);

    assertTrue(config.pack().contains("messageName"));
  }

  @Test
  void bundleExpansionCreatesChildrenAndCachesResult() {
    ProtoBufSchemaConfig config = new ProtoBufSchemaConfig();
    config.setUniqueId("root");
    config.setVersion("3");
    config.setSource("source");

    ProtoBufSchemaConfig.ProtobufConfig protobuf = new ProtoBufSchemaConfig.ProtobufConfig();
    protobuf.setMessageName("all");
    protobuf.setDescriptorValue(descriptorSet());
    config.setProtobufConfig(protobuf);

    List<SchemaConfig> children = config.getBundledSchemas();

    assertEquals(2, children.size());
    assertEquals(children, config.getBundledSchemas());
    assertTrue(children.stream().allMatch(child -> "root".equals(child.getParentUuid())));
    assertTrue(children.stream().allMatch(child -> "3".equals(child.getVersion())));
    assertTrue(children.stream().allMatch(child -> child.getUniqueId() != null));
  }

  @Test
  void getInstanceCopiesBaseConfig() {
    SchemaConfig base = new SchemaConfig();
    base.setName("copy");
    base.setFormat("protobuf");

    SchemaConfig copy = new ProtoBufSchemaConfig().getInstance(base);

    assertTrue(copy instanceof ProtoBufSchemaConfig);
    assertEquals("copy", copy.getName());
    assertEquals("application/octet-stream", copy.getMimeType());
  }

  private byte[] descriptorSet() {
    DescriptorProtos.DescriptorProto first =
        DescriptorProtos.DescriptorProto.newBuilder().setName("First").build();
    DescriptorProtos.DescriptorProto second =
        DescriptorProtos.DescriptorProto.newBuilder().setName("Second").build();

    DescriptorProtos.FileDescriptorProto file =
        DescriptorProtos.FileDescriptorProto.newBuilder()
            .setName("bundle.proto")
            .setPackage("test")
            .setSyntax("proto3")
            .addMessageType(first)
            .addMessageType(second)
            .build();

    return DescriptorProtos.FileDescriptorSet.newBuilder()
        .addFile(file)
        .build()
        .toByteArray();
  }
}
