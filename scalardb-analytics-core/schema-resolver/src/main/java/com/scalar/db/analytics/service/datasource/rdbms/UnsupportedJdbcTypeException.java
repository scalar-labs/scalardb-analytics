/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import com.scalar.db.analytics.service.datasource.SchemaResolverException;
import java.sql.JDBCType;

/** This exception is thrown when an unsupported JDBC type is encountered. */
public class UnsupportedJdbcTypeException extends SchemaResolverException {
  private static final long serialVersionUID = 3174320012206162888L;

  public UnsupportedJdbcTypeException(JdbcColumnInfo column) {
    super(
        String.format(
            "The JDBC type is not supported. "
                + "Column:%s, DataType:%s, TypeName:%s, Table:%s, Namespace:%s",
            column.getName(),
            typeString(column),
            column.getTypeName(),
            column.getTableName(),
            column.getNamespaceInfo().getNamespaceString()));
  }

  private static String typeString(JdbcColumnInfo column) {
    if (isNumericType(column)) {
      return String.format(
          "%s(%d, %d)", column.getJdbcType(), column.getSize(), column.getDigits());
    } else {
      return column.getJdbcType().getName();
    }
  }

  private static boolean isNumericType(JdbcColumnInfo column) {
    JDBCType t = column.getJdbcType();
    return t == JDBCType.BIGINT
        || t == JDBCType.DECIMAL
        || t == JDBCType.DOUBLE
        || t == JDBCType.FLOAT
        || t == JDBCType.INTEGER
        || t == JDBCType.NUMERIC
        || t == JDBCType.REAL
        || t == JDBCType.SMALLINT
        || t == JDBCType.TINYINT;
  }
}
