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

package io.mapsmessaging.schemas.formatters.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.XmlSchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.walker.MapResolver;
import io.mapsmessaging.schemas.formatters.walker.StructuredResolver;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.*;

/**
 * The type Xml formatter.
 */
public class XmlFormatter extends MessageFormatter {

  private static final String NAME = "XML";

  private final DocumentBuilder parser;
  private final String root;

  /**
   * Instantiates a new Xml formatter.
   */
  public XmlFormatter() {
    parser = null;
    root = "";
  }

  /**
   * Instantiates a new Xml formatter.
   *
   * @param config the config
   * @throws IOException the io exception
   */
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
  public JsonObject parseToJson(byte[] payload) {
    try {
      ObjectMapper xmlMapper = new XmlMapper();
      Map<String, Object> map = xmlMapper.readValue(payload, new TypeReference<>() {
      });
      if (map.size() == 1 && map.values().iterator().next() instanceof Map) {
        map = (Map<String, Object>) map.values().iterator().next();
      }

      Object cleaned = coerceTypes(map);
      JsonElement jsonElement = gson.toJsonTree(cleaned);
      JsonObject rootObject = jsonElement.getAsJsonObject();

      if (root != null && !root.isEmpty() && rootObject.has(root)) {
        return rootObject.getAsJsonObject(root);
      }

      return rootObject;
    } catch (Exception e) {
      logger.log(FORMATTER_UNEXPECTED_OBJECT, getName());
      return new JsonObject();
    }
  }

  @Override
  public synchronized ParsedObject parse(byte[] payload) {
    try {
      Document document = parser.parse(new ByteArrayInputStream(payload));
      JsonObject jsonObject = parseToJson(payload);
      Type type = new TypeToken<Map<String, Object>>() {
      }.getType();
      Map<String, Object> map = gson.fromJson(jsonObject, type);

      if (root != null && !root.isEmpty() && map.containsKey(root)) {
        map = (Map<String, Object>) map.get(root);
      }
      return new StructuredResolver(new MapResolver(map), document);
    } catch (IOException | SAXException e) {
      logger.log(XML_PARSE_EXCEPTION, getName(), e);
    }
    return new ParsedObject() {
      @Override
      public Object getReferenced() {
        return payload;
      }

      @Override
      public Object get(String s) {
        return null;
      }
    };
  }

  @Override
  public Map<String, Object> getFormat() {
    // No schema is available, so return unknown type for all fields after parsing
    // Suggest users inspect a sample payload to see actual structure
    return Map.of(
        "note", Map.of(
            "type", "unknown",
            "info", "Field structure inferred from XML payload; no schema available"
        )
    );
  }


  @Override
  public MessageFormatter getInstance(SchemaConfig config) throws IOException {
    return new XmlFormatter((XmlSchemaConfig) config);
  }

  private Object coerceTypes(Object value) {
    if (value instanceof Map) {
      Map<String, Object> result = new LinkedHashMap<>();
      ((Map<?, ?>) value).forEach((k, v) -> result.put(String.valueOf(k), coerceTypes(v)));
      return result;
    }

    if (value instanceof List) {
      List<Object> list = new ArrayList<>();
      for (Object item : (List<?>) value) {
        list.add(coerceTypes(item));
      }
      return list;
    }

    if (value instanceof String) {
      String s = (String) value;
      // Try parsing to Integer, Long, or Double
      try {
        return Integer.parseInt(s);
      } catch (NumberFormatException ignored) {
        // lets drop through, maybe long or double will work
      }
      try {
        return Long.parseLong(s);
      } catch (NumberFormatException ignored) {
        // lets drop through, maybe double will work
      }
      try {
        return Double.parseDouble(s);
      } catch (NumberFormatException ignored) {
        // assume a string
      }
      return s;
    }
    return value;
  }

}