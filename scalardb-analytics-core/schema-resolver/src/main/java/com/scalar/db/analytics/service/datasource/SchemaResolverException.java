/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource;

/**
 * This exception is thrown when an error occurs while resolving the schema of a data source. This
 * inherits the RuntimeException class because we want to use it in the lambda expressions.
 */
public class SchemaResolverException extends RuntimeException {
  private static final long serialVersionUID = -6378943649454302478L;

  public SchemaResolverException(String message) {
    super(message);
  }

  public SchemaResolverException(Throwable cause) {
    super(cause);
  }

  public SchemaResolverException(String message, Throwable cause) {
    super(message, cause);
  }
}
