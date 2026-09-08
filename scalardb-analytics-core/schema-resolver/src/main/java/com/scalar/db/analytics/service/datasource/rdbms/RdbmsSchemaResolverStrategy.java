/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import com.scalar.db.analytics.api.model.DataType;
import java.util.List;

/** This interface defines the schema resolver strategy for an RDBMS data source. */
interface RdbmsSchemaResolverStrategy {
  /**
   * Resolves the schemas in the specified database and tables in those schemas. Implementations can
   * ignore particular schemas if necessary, such as the default schemas of the database.
   */
  List<JdbcNamespaceInfo> resolveNamespaces(JdbcMetaData metaData) throws JdbcException;

  /**
   * Converts the JDBC type to the data type of the virtualized database. This method should work as
   * compatible as possible to the corresponding {@code com.scalar.db.storage.jdbc.RdbEngine} to
   * keep the consistency.
   */
  DataType jdbcTypeToDataType(JdbcColumnInfo column) throws UnsupportedJdbcTypeException;
}
