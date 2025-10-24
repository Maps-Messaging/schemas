package io.mapsmessaging.schemas.repository.impl.xregistry.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.mapsmessaging.schemas.model.XRegistrySchemaVersion;

import java.util.List;

public class XRegistryResponse {
  @JsonProperty("schemas")
  private List<XRegistrySchemaVersion> schemas;

  // Getters
  public List<XRegistrySchemaVersion> getSchemas() {
    return schemas;
  }

  // Setters
  public void setSchemas(List<XRegistrySchemaVersion> schemas) {
    this.schemas = schemas;
  }
}
