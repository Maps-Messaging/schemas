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
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MavlinkMessageFormatLoader {

  public static MavlinkMessageFormatLoader getInstance() {
    return loader;
  }

  private static final MavlinkMessageFormatLoader loader = new MavlinkMessageFormatLoader();

  private Map<String, MavlinkFormatter> dialects;

  private MavlinkMessageFormatLoader() {
    dialects = new ConcurrentHashMap<>();
    try {
      dialects.put("common", loadDefault());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public MavlinkFormatter getDialect(String dialectName) {
    return dialects.get(dialectName);
  }


  protected MavlinkFormatter loadDefault() throws IOException, ParserConfigurationException, SAXException {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream("mavlink/common.xml")) {
      return loadFromStream(stream, "common");
    }
  }

  private MavlinkFormatter loadFromStream(InputStream stream, String name) throws IOException, ParserConfigurationException, SAXException {
    MavlinkXmlParser definitionParser = new MavlinkXmlParser();
    MavlinkDialectDefinition def = definitionParser.parse(stream, name);
    MavlinkMessageRegistry registry = MavlinkMessageRegistry.fromDialectDefinition(def);
    MavlinkFormatter formatter = new MavlinkFormatter(new MavlinkPayloadPacker(registry), new MavlinkPayloadParser(registry));
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
    return formatter;
  }
}
