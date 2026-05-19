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

package io.mapsmessaging.schemas.formatters;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.SchemaConfigFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class TestJsonParsing {


  @Test
  void testJsonParsing() throws IOException {
    Map<String, Object> schemas = loadPersonsFromTestResources();
    Assertions.assertNotNull(schemas);
    Assertions.assertFalse(schemas.isEmpty());


    int[] array = {1, 2, 3, 4, 5};

    JsonArray jsonArray = new JsonArray();
    for (int j : array) {
      jsonArray.add(j);
    }

    JsonObject initialPerson = new JsonObject();
    initialPerson.addProperty("stringId", "This is a string Id");
    initialPerson.addProperty("longId", 100000L);
    initialPerson.addProperty("intId", 10);
    initialPerson.addProperty("floatId", 12.34f);
    initialPerson.addProperty("doubleId", 123456.456);
    initialPerson.addProperty("booleanId", true);
    initialPerson.addProperty("bytesId", "AQIDBAUGBwg=");
    initialPerson.add("arrayId", jsonArray);
    initialPerson.addProperty("enumId", "GREEN");
    initialPerson.addProperty("timestampId", System.currentTimeMillis());

    List<MessageFormatter> messageFormatters = createMessageFormatters(schemas);
    for (MessageFormatter messageFormatter : messageFormatters) {
      byte[] buf = messageFormatter.parseFromJson(initialPerson);
      Assertions.assertNotNull(buf);
      JsonObject rebuilt = messageFormatter.parseToJson(buf, ParseMode.IGNORE);
      Assertions.assertNotNull(rebuilt);
      Assertions.assertTrue(JsonValidator.validateJson(initialPerson, rebuilt));
    }
  }

  private List<MessageFormatter> createMessageFormatters(Map<String, Object> schemas) throws IOException {
    List<MessageFormatter> formatters = new ArrayList<>();
    for (Map.Entry<String, Object> entry : schemas.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      JsonObject properties = new JsonObject();
      properties.addProperty("format", key);
      properties.addProperty("versionId", UUID.randomUUID().toString());
      if (value instanceof String) {
        JsonObject obj = new JsonParser().parse((String) value).getAsJsonObject();
        properties.add("schema", obj);
      } else {
        Base64.Encoder encoder = Base64.getEncoder();
        String encoded = encoder.encodeToString((byte[]) value);
        if (key.equalsIgnoreCase("protoBuf")) {
          JsonObject obj = new JsonObject();
          obj.addProperty("messageName", "Person");
          obj.addProperty("descriptor", encoded);
          properties.add("schema", obj);
        } else {
          properties.addProperty("schemaBase64", encoded);
        }
      }
      SchemaConfig config = SchemaConfigFactory.getInstance().constructConfig(properties);
      if (config != null) {
        formatters.add(MessageFormatterFactory.getInstance().getFormatter(config));
      }
    }
    return formatters;
  }

  public static Map<String, Object> loadPersonsFromTestResources() throws IOException {
    Path base = Paths.get("src/test/resources");
    if (!Files.isDirectory(base)) return Map.of();

    try (Stream<Path> paths = Files.walk(base)) {
      List<Path> files = paths
          .filter(Files::isRegularFile)
          .filter(p -> p.getFileName().toString().startsWith("Person."))
          .collect(Collectors.toList());

      Map<String, Object> out = new LinkedHashMap<>();
      for (Path p : files) {
        String name = base.relativize(p).toString().replace('\\', '/');
        String key = name.substring(name.lastIndexOf('.') + 1);
        if (key.equalsIgnoreCase("proto")) {
          continue;
        }
        if (key.equalsIgnoreCase("avsc")) {
          key = "avro";
        }
        if (key.equalsIgnoreCase("desc")) {
          key = "protobuf";
        }
        if (isBinary(name)) {
          out.put(key, Files.readAllBytes(p));
        } else {
          out.put(key, Files.readString(p, StandardCharsets.UTF_8));
        }
      }
      return out;
    }
  }

  private static boolean isBinary(String name) {
    String n = name.toLowerCase(Locale.ROOT);
    return n.endsWith(".desc");
  }
}
