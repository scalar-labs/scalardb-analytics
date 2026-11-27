/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.schema;

/**
 * Thrown when the client library of a data source (such as ScalarDB or a JDBC driver) rejects the
 * configuration given in the data source's provider, for example an unsupported {@code
 * scalar.db.storage} or a JDBC URL with a malformed port, before any connection is made.
 *
 * <p>The library's exception is kept as the cause. Its message can echo configuration values, which
 * may include credentials, so it should be logged rather than returned to the caller.
 */
public class InvalidDataSourceConfigurationException extends SchemaResolverException {
  private static final long serialVersionUID = -2217315870541153170L;

  public InvalidDataSourceConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
