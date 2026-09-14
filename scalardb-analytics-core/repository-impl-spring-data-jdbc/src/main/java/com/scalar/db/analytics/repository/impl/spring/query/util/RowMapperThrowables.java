/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.query.util;

import com.scalar.db.analytics.lib.functional.Throwables;
import java.sql.ResultSet;
import org.springframework.jdbc.core.RowMapper;

public final class RowMapperThrowables {
  private RowMapperThrowables() {}

  @FunctionalInterface
  public interface ThrowingResultSetMapper<T, E extends Exception> {
    T map(ResultSet rs) throws E;
  }

  public static <T, E extends Exception> RowMapper<T> throwableRowMapper(
      ThrowingResultSetMapper<T, E> mapper) {
    return (rs, rowNum) ->
        Throwables.<ResultSet, T, E>sneakyThrowableFunction(mapper::map).apply(rs);
  }
}
