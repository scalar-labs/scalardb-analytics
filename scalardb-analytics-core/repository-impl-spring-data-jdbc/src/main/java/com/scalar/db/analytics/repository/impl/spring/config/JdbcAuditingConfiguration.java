/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.config;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

/**
 * Configuration to enable Spring Data JDBC auditing features. This allows the use of @CreatedDate
 * and @LastModifiedDate annotations.
 */
@Configuration
@EnableJdbcAuditing(dateTimeProviderRef = "instantDateTimeProvider")
public class JdbcAuditingConfiguration {

  @Bean
  public DateTimeProvider instantDateTimeProvider() {
    return () -> Optional.of(Instant.now().truncatedTo(ChronoUnit.MILLIS));
  }
}
