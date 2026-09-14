/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.analyzer;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Spring Boot FailureAnalyzer for database configuration errors during startup. Provides clear
 * indication when required database properties are missing.
 *
 * <p>This analyzer catches IllegalStateException thrown by
 * ScalarDbAnalyticsDataSourceConfiguration.dataSource() when database configuration is missing, and
 * formats it into a user-friendly error message instead of showing the full stack trace.
 */
public class DatabaseConfigurationFailureAnalyzer
    extends AbstractFailureAnalyzer<IllegalStateException> {

  @Override
  protected @Nullable FailureAnalysis analyze(Throwable rootFailure, IllegalStateException cause) {
    String message = cause.getMessage();

    // Check if this is our database configuration error thrown by
    // ScalarDbAnalyticsDataSourceConfiguration.dataSource()
    if (message != null
        && message.contains("Database connection failed during application startup")) {
      // Return the message as-is, as it's already formatted with proper instructions
      return new FailureAnalysis(message, null, cause);
    }

    return null;
  }
}
