/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import com.scalar.db.analytics.lib.functional.ThrowableFunction;
import com.scalar.db.analytics.service.datasource.SchemaResolverException;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

class JdbcUtil {
  /**
   * Iterates over the result set and applies the function to each row. This returns the list of
   * results. If the function returns null, the result is not added to the result list.
   *
   * @param rs the result set to iterate over
   * @param f the function to apply to each row. If the function returns null, the result is not
   *     added to the result list
   * @param <T> the type of the result
   * @param <E> the type of the exception
   * @return the list of results
   * @throws SchemaResolverException if an error occurs during the iteration
   */
  static <T, E extends SchemaResolverException> List<T> iterateOverResultSet(
      ResultSet rs, ThrowableFunction<ResultSet, @Nullable T, E> f) throws SchemaResolverException {
    try {
      List<T> result = new ArrayList<>();
      while (rs.next()) {
        @Nullable T value = f.apply(rs);
        if (value != null) {
          result.add(value);
        }
      }
      return result;
    } catch (SQLException e) {
      throw new JdbcException(e);
    }
  }

  /**
   * Get {@code JDBCType} of the specified {@code sqlType}. This is intentionally duplicated from
   * {@code com.scalar.db.storage.jdbc.JdbcUtil}.
   *
   * @param sqlType a type defined in {@code java.sql.Types}
   * @return a JDBCType
   */
  static JDBCType getJdbcType(int sqlType) {
    JDBCType type;
    switch (sqlType) {
      case 100: // for Oracle BINARY_FLOAT
        type = JDBCType.REAL;
        break;
      case 101: // for Oracle BINARY_DOUBLE
        type = JDBCType.DOUBLE;
        break;
      default:
        try {
          type = JDBCType.valueOf(sqlType);
        } catch (IllegalArgumentException e) {
          type = JDBCType.OTHER;
        }
    }
    return type;
  }
}
