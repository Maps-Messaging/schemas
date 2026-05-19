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

import com.google.protobuf.DescriptorProtos;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProtobufBundleMessageExtractorTest {

  @Test
  void testExtractsTopLevelAndNestedMessagesOnly() throws IOException {
    DescriptorProtos.DescriptorProto nestedMessage = DescriptorProtos.DescriptorProto.newBuilder()
        .setName("NestedMessage")
        .build();

    DescriptorProtos.DescriptorProto mapEntryMessage = DescriptorProtos.DescriptorProto.newBuilder()
        .setName("AttributesEntry")
        .setOptions(
            DescriptorProtos.MessageOptions.newBuilder()
                .setMapEntry(true)
                .build()
        )
        .build();

    DescriptorProtos.EnumDescriptorProto nestedEnum = DescriptorProtos.EnumDescriptorProto.newBuilder()
        .setName("NestedEnum")
        .addValue(
            DescriptorProtos.EnumValueDescriptorProto.newBuilder()
                .setName("VALUE_ONE")
                .setNumber(0)
                .build()
        )
        .build();

    DescriptorProtos.DescriptorProto topLevelMessage = DescriptorProtos.DescriptorProto.newBuilder()
        .setName("TopLevelMessage")
        .addNestedType(nestedMessage)
        .addNestedType(mapEntryMessage)
        .addEnumType(nestedEnum)
        .build();

    DescriptorProtos.EnumDescriptorProto topLevelEnum = DescriptorProtos.EnumDescriptorProto.newBuilder()
        .setName("TopLevelEnum")
        .addValue(
            DescriptorProtos.EnumValueDescriptorProto.newBuilder()
                .setName("ZERO")
                .setNumber(0)
                .build()
        )
        .build();

    DescriptorProtos.ServiceDescriptorProto service = DescriptorProtos.ServiceDescriptorProto.newBuilder()
        .setName("TestService")
        .build();

    DescriptorProtos.FileDescriptorProto fileDescriptorProto = DescriptorProtos.FileDescriptorProto.newBuilder()
        .setName("test.proto")
        .setPackage("test.pkg")
        .addMessageType(topLevelMessage)
        .addEnumType(topLevelEnum)
        .addService(service)
        .build();

    DescriptorProtos.FileDescriptorSet descriptorSet = DescriptorProtos.FileDescriptorSet.newBuilder()
        .addFile(fileDescriptorProto)
        .build();

    List<String> messageNames = ProtobufBundleMessageExtractor.extractMessageNames(descriptorSet.toByteArray());

    assertEquals(2, messageNames.size());
    assertTrue(messageNames.contains("test.pkg.TopLevelMessage"));
    assertTrue(messageNames.contains("test.pkg.TopLevelMessage.NestedMessage"));
    assertFalse(messageNames.contains("test.pkg.TopLevelEnum"));
    assertFalse(messageNames.contains("test.pkg.TopLevelMessage.NestedEnum"));
    assertFalse(messageNames.contains("test.pkg.TopLevelMessage.AttributesEntry"));
  }

  @Test
  void testReturnsEmptyListForNullDescriptor() throws IOException {
    List<String> messageNames = ProtobufBundleMessageExtractor.extractMessageNames(null);
    assertNotNull(messageNames);
    assertTrue(messageNames.isEmpty());
  }

  @Test
  void testReturnsEmptyListForEmptyDescriptor() throws IOException {
    List<String> messageNames = ProtobufBundleMessageExtractor.extractMessageNames(new byte[0]);
    assertNotNull(messageNames);
    assertTrue(messageNames.isEmpty());
  }

  @Test
  void testThrowsForInvalidDescriptorBytes() {
    IOException exception = assertThrows(
        IOException.class,
        () -> ProtobufBundleMessageExtractor.extractMessageNames("not-a-descriptor".getBytes())
    );
    assertTrue(exception.getMessage().contains("Failed to parse protobuf descriptor set"));
  }
}