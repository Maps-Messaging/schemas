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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.CbcSchemaConfig;
import io.mapsmessaging.schemas.config.impl.cbc.CbcFormat;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.impl.cbc.CbcInputStream;
import io.mapsmessaging.schemas.formatters.impl.cbc.CbcOutputStream;
import io.mapsmessaging.schemas.formatters.walker.MapResolver;
import io.mapsmessaging.schemas.formatters.walker.StructuredResolver;
import io.mapsmessaging.schemas.repository.SchemaResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.FORMATTER_UNEXPECTED_OBJECT;

public class CbcFormatter extends MessageFormatter {

  private final CbcFormat schema;
  private final CbcOutputStream outputStream;
  private final CbcInputStream inputStream;


  public CbcFormatter() {
    super();
    this.schema = null;
    outputStream = new CbcOutputStream();
    inputStream = new CbcInputStream();
  }

  CbcFormatter(CbcFormat schema) {
    super();
    this.schema = schema;
    outputStream = new CbcOutputStream();
    inputStream = new CbcInputStream();
  }



  public String getName() {
    return "CBC";
  }

  @Override
  public Map<String, Object> getFormat() {
    if (schema == null) {
      return Map.of();
    }
    Map<String, Object> out = new LinkedHashMap<>();
    for (io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification f : schema.getFields()) {
      out.put(f.getName(), toFieldMap(f));
    }
    return out;
  }

  @Override
  public ParsedObject parse(byte[] payload) {
    try {
      Map<String, Object> map = inputStream.decode(schema, payload);
      ParsedObject parsed = new MapResolver(map);
      return new StructuredResolver(parsed, map);
    } catch (Exception e) {
      logger.log(FORMATTER_UNEXPECTED_OBJECT, getName(), payload);
      return new DefaultParser(payload);
    }
  }

  @Override
  public synchronized JsonObject parseToJson(byte[] payload) throws IOException {
    if (schema == null) {
      throw new IllegalStateException("CBC SchemaConfig not set on formatter");
    }
    Map<String, Object> map = inputStream.decode(schema, payload);
    return new Gson().toJsonTree(map).getAsJsonObject();
  }

  @Override
  public synchronized byte[] parseFromJson(JsonObject jsonObject) throws IOException {
    if (schema == null) {
      return new byte[0];
    }
    try {
      return toBytes(jsonObject);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }


  @Override
  public MessageFormatter getInstance(SchemaConfig config, SchemaResolver schemaResolver) throws IOException {
    if (!(config instanceof CbcSchemaConfig c)) {
      throw new IllegalArgumentException("Expected CbcSchemaConfig");
    }
    return new CbcFormatter(c.getCbcFormat());
  }

  @SuppressWarnings("unchecked")
  public byte[] toBytes(Object data) {
    if (schema == null) {
      throw new IllegalStateException("CBC SchemaConfig not set on formatter");
    }
    final Map<String, Object> values;
    if (data instanceof Map<?, ?> m) {
      values = (Map<String, Object>) m;
    } else if (data instanceof JsonObject json) {
      values = new Gson().fromJson(json, Map.class);
    } else {
      throw new IllegalArgumentException("Unsupported data type for CBC encode: " + data);
    }
    return outputStream.encode(schema, values);
  }


  private static Map<String, Object> toFieldMap(io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification f) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("type", f.getType());
    if (f.getSize() != null) m.put("size", f.getSize());
    if (Boolean.TRUE.equals(f.getOptional())) m.put("optional", true);
    if (!Boolean.TRUE.equals(f.getFixed())) m.put("fixed", false);
    if (f.getDescription() != null) m.put("description", f.getDescription());
    if (f.getDecalc() != null) m.put("decalc", f.getDecalc());
    if (f.getEncalc() != null) m.put("encalc", f.getEncalc());
    if (f.getMin() != null) m.put("min", f.getMin());
    if (f.getMax() != null) m.put("max", f.getMax());

    if (f.getEnumTable() != null && !f.getEnumTable().isEmpty()) {
      Map<String, String> e = new LinkedHashMap<>();
      f.getEnumTable().entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(en -> e.put(String.valueOf(en.getKey()), en.getValue()));
      m.put("enum", e);
    }

    if (f.getFields() != null && !f.getFields().isEmpty()) {
      List<Map<String, Object>> children = new ArrayList<>();
      for (io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification c : f.getFields()) {
        Map<String, Object> child = new LinkedHashMap<>();
        child.put("name", c.getName());
        child.putAll(toFieldMap(c));
        children.add(child);
      }
      m.put("fields", children);
    }
    return m;
  }


}
