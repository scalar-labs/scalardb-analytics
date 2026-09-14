/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Common server configuration properties shared across different server components. */
@Data
@ConfigurationProperties(prefix = "scalar.db.analytics.server")
public class ServerCommonProperties {

  /**
   * Delay in milliseconds before shutting down the server after decommissioning health checks. This
   * allows load balancers and health check systems to detect that the server is going down and stop
   * routing traffic to it. Default: 10000ms (10 seconds).
   */
  private long gracefulShutdownDelayMillis = 10000;
}
