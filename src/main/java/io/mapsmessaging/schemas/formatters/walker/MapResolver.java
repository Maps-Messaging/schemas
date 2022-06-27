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

package io.mapsmessaging.schemas.formatters.walker;

import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.selector.IdentifierResolver;
import java.util.List;
import java.util.Map;

public class MapResolver implements ParsedObject {

  private final Map<String, Object> map;

  public MapResolver(Map<String, Object> map){
    this.map = map;
  }

  @Override
  public Object get(String s) {
    String lookup = s;
    boolean isArray = false;
    if(s.endsWith("]")){
      lookup = s.substring(0, s.indexOf("["));
      isArray = true;
    }
    if(map.containsKey(lookup)) {
      Object val = map.get(lookup);
      if(val instanceof List && isArray){
        String index = s.substring(s.indexOf("[")+1, s.indexOf("]"));
        var idx = Integer.parseInt(index.trim());
        if( ( (List)val).size() > idx) {
          val = ((List) val).get(idx);
        }
        else{
          return null;
        }
      }
      if(val instanceof Map){
        return new MapResolver((Map)val);
      }
      return val;
    }
    return null;
  }

  @Override
  public Object getReferenced() {
    return map;
  }
}