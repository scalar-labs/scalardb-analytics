/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.boot;

import com.scalar.db.analytics.repository.impl.spring.config.ScalarDbAnalyticsDataSourceConfiguration;
import com.scalar.db.analytics.repository.impl.spring.config.SpringDataJdbcConfiguration;
import com.scalar.db.analytics.repository.impl.spring.migration.ScalarDbSqlMigrationConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@Import({
  ScalarDbAnalyticsDataSourceConfiguration.class,
  SpringDataJdbcConfiguration.class,
  ScalarDbSqlMigrationConfiguration.class
})
public class SpringDataJdbcRepositoryConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
    return new TransactionTemplate(transactionManager);
  }
}
