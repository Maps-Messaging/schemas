/*
 *
 *        Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *        Licensed under the Apache License, Version 2.0 (the "License");
 *        you may not use this file except in compliance with the License.
 *        You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *        Unless required by applicable law or agreed to in writing, software
 *        distributed under the License is distributed on an "AS IS" BASIS,
 *        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *        See the License for the specific language governing permissions and
 *        limitations under the License.
 *
 */

package io.mapsmessaging.schemas.config.impl.meta;

import java.util.Map;
import java.util.stream.Collectors;

public class SchemaParser {

  public static Item parseItem(Map<String, Object> map) {
    String name = (String) map.get("name");
    Type type = Type.valueOf((String) map.get("type"));
    String description = (String) map.get("description");
    Access access = Access.valueOf((String) map.get("access"));
    Map<String, Object> childrenMap = (Map<String, Object>) map.get("children");
    Map<String, Item> children = childrenMap != null ? parseItems(childrenMap) : null;
    return new Item(name, type, description, access, children);
  }

  public static Map<String, Item> parseItems(Map<String, Object> map) {
    return map.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey,
            entry -> parseItem((Map<String, Object>) entry.getValue())));
  }

  public static JsonSchema parseSchema(Map<String, Object> map) {
    Map<String, Item> entries = parseItems(map);
    return new JsonSchema(entries);
  }
}
