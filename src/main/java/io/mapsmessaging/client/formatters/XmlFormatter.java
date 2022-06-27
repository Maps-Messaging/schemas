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

package io.mapsmessaging.client.formatters;

import io.mapsmessaging.client.schema.SchemaConfig;
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

  public XmlFormatter() throws IOException {
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
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

  public Object parse(byte[] payload) throws IOException {
    try {
      return parser.parse(new ByteArrayInputStream(payload));
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
    return this;
  }

}