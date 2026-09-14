/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.autoconfigure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for ScalarDB Analytics server database connection.
 *
 * <p>These properties are bound from the `scalar.db.analytics.server.db` prefix.
 */
@ConfigurationProperties(prefix = "scalar.db.analytics.server.db")
@Validated
@Data
public class DatabaseProperties {

  /** JDBC connection URL */
  @Nullable private String url;

  /** Database username */
  @Nullable private String username;

  /** Database password */
  private String password = "";

  /** JDBC driver class name. Auto-detected if not specified. */
  @Nullable private String driverClassName;

  /** Connection pool configuration */
  @Valid private final Pool pool = new Pool();

  /** Connection pool configuration properties. */
  @Data
  public static class Pool {

    /** Maximum number of connections in the pool */
    @Positive private int size = 10;

    /** Maximum lifetime of a connection in milliseconds */
    @Positive private long maxLifetime = 1800000; // 30 minutes

    /** Connection timeout in milliseconds */
    @Positive private long connectionTimeout = 30000; // 30 seconds

    /** Minimum number of idle connections */
    @Positive private int minimumIdle = 5;

    /** Idle timeout in milliseconds */
    @Positive private long idleTimeout = 600000; // 10 minutes
  }
}
