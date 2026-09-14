/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.migration;

import com.scalar.db.analytics.repository.impl.spring.autoconfigure.ScalarDbSqlProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
public class ScalarDbSqlMigrationConfiguration {

  @Bean
  public ScalarDbSqlMigrator scalarDbSqlMigrator(
      ScalarDbSqlProperties properties, PathMatchingResourcePatternResolver resolver) {
    return new ScalarDbSqlMigrator(properties, resolver);
  }

  @Bean
  public PathMatchingResourcePatternResolver migrationResourceResolver() {
    return new PathMatchingResourcePatternResolver();
  }

  @Bean
  public ScalarDbSqlMigrationRunner scalarDbSqlMigrationRunner(ScalarDbSqlMigrator migrator) {
    return new ScalarDbSqlMigrationRunner(migrator);
  }
}
