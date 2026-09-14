/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.exception;

/**
 * Exception thrown by the ScalarDB Analytics server when encountering configuration or runtime
 * errors.
 */
public class ServerException extends RuntimeException {

  public ServerException(String message) {
    super(message);
  }

  public ServerException(String message, Throwable cause) {
    super(message, cause);
  }

  public ServerException(Throwable cause) {
    super(cause);
  }
}
