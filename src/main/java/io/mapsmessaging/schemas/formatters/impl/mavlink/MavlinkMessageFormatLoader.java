/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.schemas.formatters.impl.mavlink;

import io.mapsmessaging.schemas.formatters.impl.mavlink.message.MavlinkMessageRegistry;
import io.mapsmessaging.schemas.formatters.impl.mavlink.parser.MavlinkDialectDefinition;
import io.mapsmessaging.schemas.formatters.impl.mavlink.parser.MavlinkEnumDefinition;
import io.mapsmessaging.schemas.formatters.impl.mavlink.parser.MavlinkMessageDefinition;
import io.mapsmessaging.schemas.formatters.impl.mavlink.parser.MavlinkXmlParser;

import java.io.File;
import java.io.FileInputStream;

public class MavlinkMessageFormatLoader {

  public static void main(String[] args) throws Exception {
    MavlinkXmlParser definitionParser = new MavlinkXmlParser();
    if (args.length > 0) {
      File file = new File(args[0]);
      MavlinkDialectDefinition def = definitionParser.parse(new FileInputStream(file), args[0]);
      MavlinkMessageRegistry registry = MavlinkMessageRegistry.fromDialectDefinition(def);
      MavlinkPayloadParser payloadParser = new MavlinkPayloadParser(registry);
      MavlinkPayloadPacker packer = new  MavlinkPayloadPacker(registry);
      MavlinkPayloadParser parser = new  MavlinkPayloadParser(registry);

      int fieldCount = 0;
      int enumCount = 0;
      for (MavlinkMessageDefinition definition : def.getMessages()) {
        System.out.println(definition);
        fieldCount += definition.getFields().size();
      }
      for (MavlinkEnumDefinition definition : def.getEnumsByName().values()) {
        System.out.println(definition);
        enumCount += definition.getEntries().size();
      }
      System.err.println(fieldCount + " " + enumCount);

    }
  }
}
