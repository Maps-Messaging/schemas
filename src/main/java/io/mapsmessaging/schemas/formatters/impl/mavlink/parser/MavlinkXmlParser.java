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

package io.mapsmessaging.schemas.formatters.impl.mavlink.parser;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MavlinkXmlParser {

  public MavlinkDialectDefinition parse(InputStream inputStream, String dialectName) throws Exception {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setNamespaceAware(false);
    documentBuilderFactory.setIgnoringComments(true);
    documentBuilderFactory.setIgnoringElementContentWhitespace(true);

    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
    Document document = documentBuilder.parse(inputStream);
    document.getDocumentElement().normalize();

    Map<String, MavlinkEnumDefinition> enumDefinitions = parseEnums(document);
    List<MavlinkMessageDefinition> messageDefinitions = parseMessages(document);

    Map<Integer, MavlinkMessageDefinition> messageByIdMap = new HashMap<>();
    for (MavlinkMessageDefinition messageDefinition : messageDefinitions) {
      messageByIdMap.put(messageDefinition.getMessageId(), messageDefinition);
    }

    MavlinkDialectDefinition dialectDefinition = new MavlinkDialectDefinition();
    dialectDefinition.setName(dialectName);
    dialectDefinition.setMessages(messageDefinitions);
    dialectDefinition.setMessagesById(messageByIdMap);
    dialectDefinition.setEnumsByName(enumDefinitions);

    return dialectDefinition;
  }

  private Map<String, MavlinkEnumDefinition> parseEnums(Document document) {
    Map<String, MavlinkEnumDefinition> enumMap = new HashMap<>();

    NodeList enumNodes = document.getElementsByTagName("enum");
    for (int enumIndex = 0; enumIndex < enumNodes.getLength(); enumIndex++) {
      Node enumNode = enumNodes.item(enumIndex);
      if (enumNode.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      Element enumElement = (Element) enumNode;

      MavlinkEnumDefinition enumDefinition = new MavlinkEnumDefinition();
      enumDefinition.setName(enumElement.getAttribute("name"));

      String bitmaskValue = enumElement.getAttribute("bitmask");
      boolean bitmask = "true".equalsIgnoreCase(bitmaskValue) || "1".equals(bitmaskValue);
      enumDefinition.setBitmask(bitmask);

      String descriptionText = getFirstChildTextContent(enumElement, "description");
      enumDefinition.setDescription(descriptionText);

      List<MavlinkEnumEntry> entries = new ArrayList<>();
      NodeList childNodes = enumElement.getChildNodes();
      for (int childIndex = 0; childIndex < childNodes.getLength(); childIndex++) {
        Node childNode = childNodes.item(childIndex);
        if (childNode.getNodeType() != Node.ELEMENT_NODE) {
          continue;
        }
        if (!"entry".equals(childNode.getNodeName())) {
          continue;
        }
        Element entryElement = (Element) childNode;

        MavlinkEnumEntry enumEntry = new MavlinkEnumEntry();
        String valueAttribute = entryElement.getAttribute("value");
        if (!valueAttribute.isEmpty()) {
          enumEntry.setValue(Long.parseLong(valueAttribute));
        }
        enumEntry.setName(entryElement.getAttribute("name"));
        enumEntry.setDescription(getFirstChildTextContent(entryElement, "description"));

        entries.add(enumEntry);
      }
      enumDefinition.setEntries(entries);

      enumMap.put(enumDefinition.getName(), enumDefinition);
    }

    return enumMap;
  }

  private List<MavlinkMessageDefinition> parseMessages(Document document) {
    List<MavlinkMessageDefinition> messageDefinitions = new ArrayList<>();

    NodeList messageNodes = document.getElementsByTagName("message");
    for (int messageIndex = 0; messageIndex < messageNodes.getLength(); messageIndex++) {
      Node messageNode = messageNodes.item(messageIndex);
      if (messageNode.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      Element messageElement = (Element) messageNode;

      MavlinkMessageDefinition messageDefinition = new MavlinkMessageDefinition();
      messageDefinition.setMessageId(Integer.parseInt(messageElement.getAttribute("id")));
      messageDefinition.setName(messageElement.getAttribute("name"));
      messageDefinition.setDescription(getFirstChildTextContent(messageElement, "description"));

      List<MavlinkFieldDefinition> fieldDefinitions = new ArrayList<>();
      NodeList childNodes = messageElement.getChildNodes();
      int fieldIndex = 0;
      for (int childIndex = 0; childIndex < childNodes.getLength(); childIndex++) {
        Node childNode = childNodes.item(childIndex);
        if (childNode.getNodeType() != Node.ELEMENT_NODE) {
          continue;
        }
        if (!"field".equals(childNode.getNodeName())) {
          continue;
        }
        Element fieldElement = (Element) childNode;

        MavlinkFieldDefinition fieldDefinition = new MavlinkFieldDefinition();
        fieldDefinition.setIndex(fieldIndex++);
        fieldDefinition.setType(fieldElement.getAttribute("type"));
        fieldDefinition.setName(fieldElement.getAttribute("name"));
        fieldDefinition.setUnits(fieldElement.getAttribute("units"));
        fieldDefinition.setDescription(fieldElement.getTextContent().trim());

        String enumName = fieldElement.getAttribute("enum");
        if (!enumName.isEmpty()) {
          fieldDefinition.setEnumName(enumName);
        }

        fieldDefinitions.add(fieldDefinition);
      }

      messageDefinition.setFields(fieldDefinitions);
      messageDefinitions.add(messageDefinition);
    }

    return messageDefinitions;
  }

  private String getFirstChildTextContent(Element parentElement, String tagName) {
    NodeList nodeList = parentElement.getElementsByTagName(tagName);
    if (nodeList.getLength() == 0) {
      return null;
    }
    Node node = nodeList.item(0);
    if (node == null) {
      return null;
    }
    String text = node.getTextContent();
    if (text == null) {
      return null;
    }
    return text.trim();
  }

}
