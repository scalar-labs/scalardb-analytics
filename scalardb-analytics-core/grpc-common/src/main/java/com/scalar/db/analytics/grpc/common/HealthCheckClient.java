/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.common;

import com.scalar.db.analytics.grpc.common.exception.HealthCheckException;

/**
 * Client for checking the health status of a gRPC server.
 *
 * <p>This interface provides methods to verify the health of the entire server or specific services
 * using the gRPC Health Checking Protocol.
 */
public interface HealthCheckClient {

  /**
   * Checks the overall health of the server.
   *
   * <p>This is equivalent to calling {@code check("")} with an empty service name.
   *
   * @return true if the server is healthy (SERVING status), false if the server explicitly reports
   *     an unhealthy state (NOT_SERVING)
   * @throws HealthCheckException if the health check fails due to communication errors, timeouts,
   *     or other exceptions
   */
  boolean check() throws HealthCheckException;

  /**
   * Checks the health of a specific service.
   *
   * @param serviceName the fully-qualified service name (e.g., "catalog.v1.CatalogService"). Use an
   *     empty string to check the overall server health.
   * @return true if the service is healthy (SERVING status), false if the service explicitly
   *     reports an unhealthy state (NOT_SERVING)
   * @throws HealthCheckException if the health check fails due to communication errors, timeouts,
   *     or other exceptions
   */
  boolean check(String serviceName) throws HealthCheckException;
}
