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
package io.mapsmessaging.schemas.config;

import io.mapsmessaging.schemas.model.XRegistrySchemaVersion;

public class SchemaConfig extends XRegistrySchemaVersion {
  protected SchemaConfig(String format) {
    super(format);
  }

  protected SchemaConfig(XRegistrySchemaVersion copyFrom) {
    super(copyFrom);
  }

  public void setVersion(int val) {
    super.setVersion("" + val);
  }

  public void setTitle(String val) {
    setName(val);
  }

  public String getTitle() {
    return getName();
  }

  public SchemaConfig getInstance(XRegistrySchemaVersion config) {
    return null;
  }

  public String getMimeType() {
    return null;
  }

}
