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

import io.mapsmessaging.selector.IdentifierResolver;
import java.math.BigDecimal;
import java.util.List;

public class StructureWalker {


  private StructureWalker() {
  }

  public static Object locateObject(IdentifierResolver resolver, List<String> searchPath) {
    Object context = null;
    while (!searchPath.isEmpty()) {
      var path = searchPath.remove(0);
      context = resolver.get(path);
      if (context instanceof IdentifierResolver) {
        resolver = (IdentifierResolver) context;
      }
    }
    return parse(context);
  }

  private static Object parse(Object lookup) {
    if (lookup == null) {
      return null;
    }
    if (lookup instanceof String ||
        lookup instanceof Float ||
        lookup instanceof Double ||
        lookup instanceof Byte ||
        lookup instanceof Character ||
        lookup instanceof Short ||
        lookup instanceof Integer ||
        lookup instanceof Long) {
      return lookup;
    } else if (lookup instanceof BigDecimal) {
      return ((BigDecimal) lookup).doubleValue();
    }
    return lookup.toString();
  }
}
