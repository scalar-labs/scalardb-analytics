/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.common.exception;

/**
 * Exception thrown when a health check operation fails due to communication errors or server
 * issues.
 *
 * <p>This exception indicates that the health check could not be performed successfully, such as
 * network errors, timeouts, or server unavailability. It is distinct from a health check returning
 * false (NOT_SERVING), which indicates the service is explicitly reporting an unhealthy state.
 */
public class HealthCheckException extends Exception {
  private static final long serialVersionUID = 1L;

  public HealthCheckException(String message) {
    super(message);
  }

  public HealthCheckException(String message, Throwable cause) {
    super(message, cause);
  }
}
