/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.exception;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for error mapping components. Selects the appropriate database-specific error
 * analyzer based on the database type.
 */
@Configuration
public class ErrorMapperConfiguration {

  @Bean
  public DatabaseSpecificErrorAnalyzer databaseSpecificErrorAnalyzer() {
    return new ScalarDbSqlErrorAnalyzer();
  }
}
