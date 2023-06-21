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

/**
 * Class representing a single entry in a JSON object.
 * Each Item has a name, type, description, access level, and possibly child Items.
 */
@Value
public class Item {
  String name;
  Type type;
  String description;
  Access access;
  Map<String, Item> children;

  public Item(String name, Type type, String description, Access access, Map<String, Item> children) {
    this.name = name;
    this.type = type;
    this.description = description;
    this.access = access;
    this.children = children;
  }
}


