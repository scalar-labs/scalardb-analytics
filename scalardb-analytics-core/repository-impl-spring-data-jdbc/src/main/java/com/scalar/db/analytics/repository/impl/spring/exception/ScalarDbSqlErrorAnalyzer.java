/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.exception;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.sql.exception.SqlException;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataAccessException;

/**
 * Minimal error analyzer for ScalarDB SQL. Currently delegates to the generic mapper unless the
 * root cause is a ScalarDB {@link SqlException}, in which case the message is surfaced as a
 * database error.
 */
public class ScalarDbSqlErrorAnalyzer implements DatabaseSpecificErrorAnalyzer {

  @Override
  public Optional<AnalyticsException> categorizeError(
      DataAccessException e, String entityType, @Nullable String identifier) {
    Throwable root = e.getMostSpecificCause();
    if (root instanceof SqlException sqlException) {
      return Optional.of(
          new AnalyticsException(AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED, e));
    }
    return Optional.empty();
  }
}
