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

import com.google.protobuf.DescriptorProtos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DescriptorInspector {

  public static void inspect(Path descriptorPath) throws IOException {
    byte[] bytes = Files.readAllBytes(descriptorPath);
    DescriptorProtos.FileDescriptorSet fileDescriptorSet =
        DescriptorProtos.FileDescriptorSet.parseFrom(bytes);

    System.out.println("Descriptor: " + descriptorPath);
    System.out.println("Files in set: " + fileDescriptorSet.getFileCount());

    for (DescriptorProtos.FileDescriptorProto fileDescriptor : fileDescriptorSet.getFileList()) {
      System.out.println("File: " + fileDescriptor.getName());

      for (DescriptorProtos.DescriptorProto messageType : fileDescriptor.getMessageTypeList()) {
        System.out.println("  Message: " + messageType.getName());
      }

      for (DescriptorProtos.EnumDescriptorProto enumType : fileDescriptor.getEnumTypeList()) {
        System.out.println("  Enum: " + enumType.getName());
      }

      for (DescriptorProtos.ServiceDescriptorProto service : fileDescriptor.getServiceList()) {
        System.out.println("  Service: " + service.getName());
      }
    }
  }


}
