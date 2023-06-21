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

import org.json.JSONObject;

import java.util.Map;

public class SchemaSerializer {
  /**
   * Serializes a single Item into a JSONObject.
   *
   * @param item The Item to serialize.
   * @return A JSONObject representing the Item.
   */
  public static JSONObject serializeItem(Item item) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("name", item.getName());
    jsonObject.put("type", item.getType().getJsonType());
    jsonObject.put("description", item.getDescription());
    jsonObject.put("access", item.getAccess().getAccessType());
    JSONObject children = item.getChildren() != null ?
        serializeItems(item.getChildren()) : null;
    jsonObject.put("children", children);
    return jsonObject;
  }

  /**
   * Serializes a map of Items into a JSONObject.
   *
   * @param items The map of Items to serialize.
   * @return A JSONObject representing the map of Items.
   */
  public static JSONObject serializeItems(Map<String, Item> items) {
    JSONObject jsonObject = new JSONObject();
    for (Map.Entry<String, Item> entry : items.entrySet()) {
      jsonObject.put(entry.getKey(), serializeItem(entry.getValue()));
    }
    return jsonObject;
  }

  /**
   * Serializes a JsonSchema into a JSONObject.
   *
   * @param schema The JsonSchema to serialize.
   * @return A JSONObject representing the JsonSchema.
   */
  public static JSONObject serializeSchema(JsonSchema schema) {
    return serializeItems(schema.getEntries());
  }
}