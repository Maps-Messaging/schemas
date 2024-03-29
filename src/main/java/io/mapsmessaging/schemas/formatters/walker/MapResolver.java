/*
 *
 *     Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

package io.mapsmessaging.schemas.formatters.walker;

import io.mapsmessaging.schemas.formatters.ParsedObject;
import java.util.List;
import java.util.Map;

/**
 * The type Map resolver.
 */
public class MapResolver implements ParsedObject {

  private final Map<String, Object> map;
  private final boolean parseStringNumerics;

  /**
   * Instantiates a new Map resolver.
   *
   * @param map the map
   */
  public MapResolver(Map<String, Object> map) {
    this(map, false);
  }

  /**
   * Instantiates a new Map resolver.
   *
   * @param map the map
   * @param parseStringNumerics the parse string numerics
   */
  public MapResolver(Map<String, Object> map, boolean parseStringNumerics) {
    this.map = map;
    this.parseStringNumerics = parseStringNumerics;
  }


  @Override
  public Object get(String s) {
    String lookup = s;
    boolean isArray = false;
    if (s.endsWith("]")) {
      lookup = s.substring(0, s.indexOf("["));
      isArray = true;
    }
    if (map.containsKey(lookup)) {
      Object val = map.get(lookup);
      if (val instanceof List && isArray) {
        String index = s.substring(s.indexOf("[") + 1, s.indexOf("]"));
        var idx = Integer.parseInt(index.trim());
        List<Object> vList = (List<Object>) val;
        if (vList.size() > idx) {
          val = vList.get(idx);
        } else {
          return null;
        }
      }
      return parseValue(val);
    }
    return null;
  }

  private Object parseValue(Object val) {
    if (val instanceof Map) {
      return new MapResolver((Map<String, Object>) val);
    }
    if (val instanceof String && parseStringNumerics) {
      try {
        return Long.parseLong((String) val);
      } catch (NumberFormatException e) {
        // We can ignore this since we don't actually know if it is numeric or not
      }
      try {
        return Double.parseDouble((String) val);
      } catch (NumberFormatException e) {
        // We can ignore this since we don't actually know if it is numeric or not
      }
    }
    return val;
  }

  @Override
  public Object getReferenced() {
    return map;
  }
}