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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ProtobufBundleMessageExtractor {

  private ProtobufBundleMessageExtractor() {
  }

  public static List<String> extractMessageNames(byte[] descriptorValue) throws IOException {
    if (descriptorValue == null || descriptorValue.length == 0) {
      return Collections.emptyList();
    }

    try {
      DescriptorProtos.FileDescriptorSet fileDescriptorSet = DescriptorProtos.FileDescriptorSet.parseFrom(descriptorValue);
      List<String> messageNames = new ArrayList<>();

      for (DescriptorProtos.FileDescriptorProto fileDescriptorProto : fileDescriptorSet.getFileList()) {
        String packageName = fileDescriptorProto.getPackage();
        for (DescriptorProtos.DescriptorProto descriptorProto : fileDescriptorProto.getMessageTypeList()) {
          collectMessageNames(packageName, null, descriptorProto, messageNames);
        }
      }
      return Collections.unmodifiableList(messageNames);
    } catch (Exception e) {
      throw new IOException("Failed to parse protobuf descriptor set", e);
    }
  }

  private static void collectMessageNames(
      String packageName,
      String parentMessageName,
      DescriptorProtos.DescriptorProto descriptorProto,
      List<String> messageNames) {

    if (descriptorProto.hasOptions() && descriptorProto.getOptions().getMapEntry()) {
      return;
    }

    String currentName = buildName(packageName, parentMessageName, descriptorProto.getName());
    messageNames.add(currentName);

    for (DescriptorProtos.DescriptorProto nestedDescriptor : descriptorProto.getNestedTypeList()) {
      collectMessageNames(packageName, currentNameWithoutPackage(packageName, currentName), nestedDescriptor, messageNames);
    }
  }

  private static String buildName(String packageName, String parentMessageName, String messageName) {
    StringBuilder stringBuilder = new StringBuilder();

    if (packageName != null && !packageName.isEmpty()) {
      stringBuilder.append(packageName).append('.');
    }

    if (parentMessageName != null && !parentMessageName.isEmpty()) {
      stringBuilder.append(parentMessageName).append('.');
    }

    stringBuilder.append(messageName);
    return stringBuilder.toString();
  }

  private static String currentNameWithoutPackage(String packageName, String fullName) {
    if (packageName == null || packageName.isEmpty()) {
      return fullName;
    }

    String prefix = packageName + ".";
    if (fullName.startsWith(prefix)) {
      return fullName.substring(prefix.length());
    }
    return fullName;
  }
}