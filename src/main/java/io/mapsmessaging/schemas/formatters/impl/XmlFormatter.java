/*
 *
 *     Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package io.mapsmessaging.schemas.formatters.impl;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.XML_CONFIGURATION_EXCEPTION;
import static io.mapsmessaging.schemas.logging.SchemaLogMessages.XML_PARSE_EXCEPTION;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.XmlSchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.walker.MapResolver;
import io.mapsmessaging.schemas.formatters.walker.StructuredResolver;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XmlFormatter extends MessageFormatter {

  private static final String NAME = "XML";

  private final DocumentBuilder parser;
  private final String root;

  public XmlFormatter() {
    parser = null;
    root = "";
  }

  XmlFormatter(XmlSchemaConfig config) throws IOException {
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(config.isNamespaceAware());
      dbf.setValidating(config.isValidating());
      dbf.setCoalescing(config.isCoalescing());
      parser = dbf.newDocumentBuilder();
      root = config.getRootEntry();
    } catch (ParserConfigurationException e) {
      logger.log(XML_CONFIGURATION_EXCEPTION, e);
      throw new IOException(e);
    }
  }

  public String getName() {
    return NAME;
  }

  @Override
  public JSONObject parseToJson(byte[] payload) {
    JSONObject jsonObject = XML.toJSONObject(new String(payload));
    if (root != null && root.length() > 0) {
      jsonObject = jsonObject.getJSONObject(root);
    }
    return jsonObject;
  }

  @Override
  public synchronized ParsedObject parse(byte[] payload) {
    try {
      Document document = parser.parse(new ByteArrayInputStream(payload));
      Map<String, Object> map = parseToJson(payload).toMap();
      if (root != null && root.length() > 0 && map.containsKey(root)) {
        map = (Map<String, Object>) map.get(root);
      }
      return new StructuredResolver(new MapResolver(map), document);
    } catch (IOException | SAXException e) {
      logger.log(XML_PARSE_EXCEPTION, getName(), e);
    }
    return null;
  }

  @Override
  public MessageFormatter getInstance(SchemaConfig config) throws IOException {
    return new XmlFormatter((XmlSchemaConfig) config);
  }

}