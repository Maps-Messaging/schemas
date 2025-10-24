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

package io.mapsmessaging.schemas.config.impl;

import io.mapsmessaging.schemas.model.XRegistrySchemaVersion;

public class CborSchemaConfig extends XRegistrySchemaVersionImpl {

  private static final String NAME = "CBOR";

  public CborSchemaConfig() {
    super(NAME);
  }


  public CborSchemaConfig(XRegistrySchemaVersion config) {
    super(config);
  }

  @Override
  public String getMimeType() {
    return "application/cbor";
  }

  @Override
  public XRegistrySchemaVersion getInstance(XRegistrySchemaVersion config) {
    return new CborSchemaConfig(config);
  }

}
