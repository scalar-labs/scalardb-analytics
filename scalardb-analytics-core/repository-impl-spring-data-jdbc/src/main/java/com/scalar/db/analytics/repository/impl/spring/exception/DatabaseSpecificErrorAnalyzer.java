/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.exception;

import com.scalar.db.analytics.api.error.AnalyticsException;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataAccessException;

/**
 * Database-specific error analysis strategy interface. Handles database-specific error patterns.
 */
public interface DatabaseSpecificErrorAnalyzer {

  /**
   * Analyzes error message for database-specific patterns. Only handles patterns that can't be
   * determined by SQLState alone.
   *
   * @param e the exception to analyze
   * @param entityType the entity type involved
   * @param identifier the entity identifier (can be null)
   * @return specific AnalyticsException if pattern matches, empty otherwise
   */
  Optional<AnalyticsException> categorizeError(
      DataAccessException e, String entityType, @Nullable String identifier);
}
