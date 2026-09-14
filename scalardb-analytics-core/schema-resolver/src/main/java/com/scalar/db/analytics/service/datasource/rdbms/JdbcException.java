/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import com.scalar.db.analytics.service.datasource.SchemaResolverException;
import java.sql.SQLException;

/** This exception is thrown when an unhandled error occurs during JDBC operations. */
public class JdbcException extends SchemaResolverException {
  private static final long serialVersionUID = 2474793796055967737L;

  public JdbcException(SQLException cause) {
    super(cause);
  }
}
