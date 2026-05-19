/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.schemas.tools.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class JsonSchemaPointerResolver {

  private JsonSchemaPointerResolver() {
  }

  public static JsonElement resolve(JsonObject rootSchema, String pointer) {
    if (rootSchema == null) {
      throw new IllegalArgumentException("Root schema is null");
    }
    if (pointer == null || pointer.isEmpty()) {
      throw new IllegalArgumentException("Pointer is null or empty");
    }
    if ("#".equals(pointer)) {
      return rootSchema;
    }
    if (!pointer.startsWith("#/")) {
      throw new IllegalArgumentException("Invalid JSON pointer: " + pointer);
    }

    JsonElement current = rootSchema;
    String[] parts = pointer.substring(2).split("/");

    for (String part : parts) {
      String token = unescape(part);
      if (!current.isJsonObject()) {
        throw new IllegalArgumentException("Pointer does not resolve to an object path: " + pointer);
      }

      JsonObject object = current.getAsJsonObject();
      if (!object.has(token)) {
        throw new IllegalArgumentException("Pointer token not found: " + token + " in " + pointer);
      }

      current = object.get(token);
    }

    return current;
  }

  public static JsonObject resolveObject(JsonObject rootSchema, String pointer) {
    JsonElement element = resolve(rootSchema, pointer);
    if (!element.isJsonObject()) {
      throw new IllegalArgumentException("Resolved pointer is not a JsonObject: " + pointer);
    }
    return element.getAsJsonObject();
  }

  private static String unescape(String token) {
    return token.replace("~1", "/").replace("~0", "~");
  }
}