/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 *
 */

package io.mapsmessaging.schemas.repository.impl;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.SchemaResource;
import io.mapsmessaging.schemas.repository.SchemaRepository;
import org.junit.jupiter.api.Test;

class SchemaRepositoryResolverTest {

  @Test
  void resolveParentReturnsDefaultVersionFromRepository() {
    SchemaRepository repository = mock(SchemaRepository.class);
    SchemaRepositoryResolver resolver = new SchemaRepositoryResolver(repository);
    SchemaConfig child = new SchemaConfig();
    child.setParentUuid("parent");

    SchemaConfig parent = new SchemaConfig();
    SchemaResource resource = new SchemaResource();
    resource.setSchemaId("parent");
    parent.setVersion("1");
    resource.setDefaultVersion(parent);

    when(repository.getResource("parent")).thenReturn(resource);

    assertSame(parent, resolver.resolveParent(child));
    verify(repository).getResource("parent");
  }

  @Test
  void resolveParentReturnsNullWhenResourceIsMissing() {
    SchemaRepository repository = mock(SchemaRepository.class);
    SchemaRepositoryResolver resolver = new SchemaRepositoryResolver(repository);
    SchemaConfig child = new SchemaConfig();
    child.setParentUuid("missing");

    when(repository.getResource("missing")).thenReturn(null);

    assertNull(resolver.resolveParent(child));
  }
}
