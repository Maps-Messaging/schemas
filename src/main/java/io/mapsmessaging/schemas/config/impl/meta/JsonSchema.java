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

import lombok.Value;

import java.util.Map;
import java.util.Optional;

/**
 * Class representing a JSON object as a schema.
 * The schema consists of a map of entries, where each entry is a key-value pair of a name and an Item.
 */
@Value
public class JsonSchema {
  private final Map<String, Item> entries;

  public JsonSchema(Map<String, Item> entries) {
    this.entries = entries;
  }

  public Optional<Item> getEntry(String key) {
    return Optional.ofNullable(entries.get(key));
  }
}

