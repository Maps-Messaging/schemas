/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package io.mapsmessaging.schemas.config.impl;

import io.mapsmessaging.schemas.config.SchemaConfig;

public class CdrSchemaConfig extends SchemaConfig {

  public CdrSchemaConfig() {
    super("cdr");
  }

  public CdrSchemaConfig(SchemaConfig config) {
    super(config);
  }

  @Override
  public String getMimeType() {
    return "application/cdr";
  }

  @Override
  public SchemaConfig getInstance(SchemaConfig config) {
    return new CdrSchemaConfig(config);
  }
}
