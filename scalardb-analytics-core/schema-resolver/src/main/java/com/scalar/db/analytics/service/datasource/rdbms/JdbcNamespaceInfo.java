/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * This interface provides the namespace information of a JDBC data source. This also provides the
 * mapping between the namespace names of the application and the catalog and schema names of the
 * JDBC data source.
 */
interface JdbcNamespaceInfo {
  /** Returns the namespace names of this application. */
  List<String> toNamespaceNames();

  /** Return the catalog name for the JDBC DatabaseMetaData. */
  @Nullable String jdbcCatalog();

  /** Return the schema name for the JDBC DatabaseMetaData. */
  @Nullable String jdbcSchema();

  /**
   * Returns the string representation of the application's namespace. This is intended to be used
   * as logging or debugging information. Do not use this for any other purpose.
   */
  default String getNamespaceString() {
    return String.join(".", toNamespaceNames());
  }
}
