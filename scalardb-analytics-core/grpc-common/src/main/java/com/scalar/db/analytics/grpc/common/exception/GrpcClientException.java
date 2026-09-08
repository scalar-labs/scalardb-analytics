/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.common.exception;

/**
 * Exception thrown when gRPC client operations fail.
 *
 * <p>This exception wraps transport-level errors, runtime failures, and client-specific validation
 * errors.
 */
public class GrpcClientException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public GrpcClientException(String message) {
    super(message);
  }

  public GrpcClientException(Throwable cause) {
    super(cause);
  }

  public GrpcClientException(String message, Throwable cause) {
    super(message, cause);
  }
}
