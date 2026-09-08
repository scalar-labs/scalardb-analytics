/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import java.sql.DatabaseMetaData;
import java.sql.JDBCType;
import lombok.Value;

/** This class is a container for the information of a column in a JDBC data source. */
@Value
class JdbcColumnInfo {
  JdbcNamespaceInfo namespaceInfo;
  String tableName;
  String name;
  JDBCType jdbcType;
  String typeName;
  int size;
  int digits;
  int nullable;

  /**
   * Returns the fully qualified name of the column. This is intended to be used as logging or
   * debugging information. Do not use this for any other purpose.
   */
  String getQualifiedName() {
    String namespace = String.join(".", namespaceInfo.toNamespaceNames());
    return namespace + "." + tableName + "." + name;
  }

  String getNumericTypeDescription() {
    return String.format("%s(%d, %d)", typeName, size, digits);
  }

  boolean isNullable() {
    return nullable != DatabaseMetaData.columnNoNulls;
  }
}
