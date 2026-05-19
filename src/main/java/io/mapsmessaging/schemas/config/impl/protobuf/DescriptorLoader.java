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

package io.mapsmessaging.schemas.config.impl.protobuf;


import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class DescriptorLoader {

  public Map<String, Descriptors.FileDescriptor> loadDescFiles(byte[] descriptorImage)
      throws IOException, Descriptors.DescriptorValidationException {
    DescriptorProtos.FileDescriptorSet fileDescriptorSet;

    try (InputStream inputStream = new ByteArrayInputStream(descriptorImage)) {
      fileDescriptorSet = DescriptorProtos.FileDescriptorSet.parseFrom(inputStream);
    }

    Map<String, DescriptorProtos.FileDescriptorProto> protoByName = new LinkedHashMap<>();
    Map<String, Descriptors.FileDescriptor> builtDescriptors = new LinkedHashMap<>();

    for (DescriptorProtos.FileDescriptorProto fileDescriptorProto : fileDescriptorSet.getFileList()) {
      protoByName.put(fileDescriptorProto.getName(), fileDescriptorProto);
    }

    boolean progress = true;
    while (!protoByName.isEmpty() && progress) {
      progress = false;

      List<String> pendingNames = new ArrayList<>(protoByName.keySet());
      for (String fileName : pendingNames) {
        DescriptorProtos.FileDescriptorProto fileDescriptorProto = protoByName.get(fileName);

        if (dependenciesResolved(fileDescriptorProto, builtDescriptors)) {
          Descriptors.FileDescriptor[] dependencies = resolveDependencies(fileDescriptorProto, builtDescriptors);
          Descriptors.FileDescriptor fileDescriptor = Descriptors.FileDescriptor.buildFrom(fileDescriptorProto, dependencies);

          builtDescriptors.put(fileName, fileDescriptor);
          protoByName.remove(fileName);
          progress = true;
        }
      }
    }

    if (!protoByName.isEmpty()) {
      throw new IllegalStateException("Unable to resolve protobuf descriptor dependencies for files: " + protoByName.keySet());
    }

    return Collections.unmodifiableMap(builtDescriptors);
  }

  public Map<String, Descriptors.Descriptor> loadMessageDescriptors(byte[] descriptorImage)
      throws IOException, Descriptors.DescriptorValidationException {
    Map<String, Descriptors.FileDescriptor> fileDescriptorMap = loadDescFiles(descriptorImage);
    Map<String, Descriptors.Descriptor> messageDescriptorMap = new LinkedHashMap<>();

    for (Descriptors.FileDescriptor fileDescriptor : fileDescriptorMap.values()) {
      for (Descriptors.Descriptor descriptor : fileDescriptor.getMessageTypes()) {
        addMessageDescriptor(descriptor, messageDescriptorMap);
      }
    }

    return Collections.unmodifiableMap(messageDescriptorMap);
  }

  public Descriptors.Descriptor findMessageDescriptor(byte[] descriptorImage, String fullName)
      throws IOException, Descriptors.DescriptorValidationException {
    Map<String, Descriptors.Descriptor> messageDescriptorMap = loadMessageDescriptors(descriptorImage);
    return messageDescriptorMap.get(fullName);
  }

  private boolean dependenciesResolved(
      DescriptorProtos.FileDescriptorProto fileDescriptorProto,
      Map<String, Descriptors.FileDescriptor> builtDescriptors) {
    for (String dependencyName : fileDescriptorProto.getDependencyList()) {
      if (!builtDescriptors.containsKey(dependencyName)) {
        return false;
      }
    }
    return true;
  }

  private Descriptors.FileDescriptor[] resolveDependencies(
      DescriptorProtos.FileDescriptorProto fileDescriptorProto,
      Map<String, Descriptors.FileDescriptor> builtDescriptors) {
    List<Descriptors.FileDescriptor> dependencies = new ArrayList<>();

    for (String dependencyName : fileDescriptorProto.getDependencyList()) {
      Descriptors.FileDescriptor dependency = builtDescriptors.get(dependencyName);
      if (dependency == null) {
        throw new IllegalStateException("Missing protobuf dependency: " + dependencyName + " required by " + fileDescriptorProto.getName());
      }
      dependencies.add(dependency);
    }

    return dependencies.toArray(new Descriptors.FileDescriptor[0]);
  }

  private void addMessageDescriptor(
      Descriptors.Descriptor descriptor,
      Map<String, Descriptors.Descriptor> messageDescriptorMap) {
    messageDescriptorMap.put(descriptor.getFullName(), descriptor);

    for (Descriptors.Descriptor nestedDescriptor : descriptor.getNestedTypes()) {
      addMessageDescriptor(nestedDescriptor, messageDescriptorMap);
    }
  }
}