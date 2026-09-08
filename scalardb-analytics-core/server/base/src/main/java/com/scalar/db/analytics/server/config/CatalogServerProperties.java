/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@ConfigurationProperties(prefix = "scalar.db.analytics.server.catalog")
@Validated
public class CatalogServerProperties {

  private static final int DEFAULT_PORT = 11051;

  @Min(1)
  @Max(65535)
  private final int port;

  public CatalogServerProperties(@Nullable Integer port) {
    this.port = port != null ? port : DEFAULT_PORT;
  }
}
