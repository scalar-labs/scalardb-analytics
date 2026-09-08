/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.analyzer;

import com.zaxxer.hikari.pool.HikariPool;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Spring Boot FailureAnalyzer for database connection errors during startup. Provides clear
 * indication that the database connection failed during application startup.
 */
public class DatabaseConnectionFailureAnalyzer
    extends AbstractFailureAnalyzer<HikariPool.PoolInitializationException> {

  @Override
  protected FailureAnalysis analyze(
      Throwable rootFailure, HikariPool.PoolInitializationException cause) {

    Throwable rootCause = getRootCause(cause);
    String dbError = rootCause.getMessage();

    String description = "Database connection failed during application startup.";

    // Include the original database error message
    if (dbError != null && !dbError.isEmpty()) {
      description += String.format("%n%nDatabase error: %s", dbError);
    }

    String action =
        String.format(
            "Please verify:%n"
                + "1. Database server is running and accessible%n"
                + "2. Database exists and is properly configured%n"
                + "3. Connection parameters in application.properties:%n"
                + "   - scalar.db.analytics.server.db.contact-points%n"
                + "   - scalar.db.analytics.server.db.username%n"
                + "   - scalar.db.analytics.server.db.password");

    return new FailureAnalysis(description, action, cause);
  }

  private Throwable getRootCause(Throwable throwable) {
    Throwable root = throwable;
    while (root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }
    return root;
  }
}
