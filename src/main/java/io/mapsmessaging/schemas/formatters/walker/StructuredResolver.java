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
import java.util.ArrayList;
import java.util.List;

/**
 * The type Structured resolver.
 */
public class StructuredResolver implements ParsedObject {

  private final IdentifierResolver resolver;
  private final Object reference;

  /**
   * Instantiates a new Structured resolver.
   *
   * @param resolver the resolver
   * @param reference the reference
   */
  public StructuredResolver(IdentifierResolver resolver, Object reference) {
    this.resolver = resolver;
    this.reference = reference;
  }

  @Override
  public Object get(String s) {
    List<String> keys = new ArrayList<>();

    if (s.contains(".")) {
      keys.addAll(List.of(s.split("\\.")));
    } else {
      keys.add(s);
    }
    return StructureWalker.locateObject(resolver, keys);
  }

  @Override
  public Object getReferenced() {
    return reference;
  }
}