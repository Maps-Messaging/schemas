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

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.XmlSchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.walker.MapResolver;
import io.mapsmessaging.schemas.formatters.walker.StructuredResolver;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XmlFormatter implements MessageFormatter {

  private final DocumentBuilder parser;

  public XmlFormatter() {
    parser = null;
  }

  XmlFormatter(XmlSchemaConfig config) throws IOException {
    try {

      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(config.isNamespaceAware());
      dbf.setValidating(config.isValidating());
      dbf.setCoalescing(config.isCoalescing());
      parser = dbf.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new IOException(e);
    }
  }


  public String getName() {
    return "XML";
  }

  @Override
  public JSONObject parseToJson(byte[] payload) {
    return XML.toJSONObject(new String(payload));
  }

  public ParsedObject parse(byte[] payload) throws IOException {
    try {
      Document document = parser.parse(new ByteArrayInputStream(payload));
      return new StructuredResolver(new MapResolver(parseToJson(payload).toMap()), document);
    } catch (SAXException e) {
      throw new IOException(e);
    }
  }

  public byte[] pack(Object object) throws IOException {
    String toPack = null;
    if (object instanceof String) {
      toPack = (String) object;
    }
    if (object instanceof Document) {
      try {
        DOMSource domSource = new DOMSource((Document) object);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.transform(domSource, result);
        toPack = writer.toString();
      } catch (TransformerException e) {
        throw new IOException(e);
      }
    }
    if (toPack != null) {
      return toPack.getBytes(StandardCharsets.UTF_8);
    }
    throw new IOException("Unexpected object to be packed");
  }

  @Override
  public MessageFormatter getInstance(SchemaConfig config) throws IOException {
    return new XmlFormatter( (XmlSchemaConfig) config);
  }

}