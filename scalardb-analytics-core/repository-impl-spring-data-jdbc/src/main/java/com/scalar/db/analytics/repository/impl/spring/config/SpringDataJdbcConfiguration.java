/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.repository.impl.spring.converter.reading.JsonToStringListConverter;
import com.scalar.db.analytics.repository.impl.spring.converter.reading.StringToUuidConverter;
import com.scalar.db.analytics.repository.impl.spring.converter.writing.StringListToJsonConverter;
import com.scalar.db.analytics.repository.impl.spring.converter.writing.UuidToStringConverter;
import com.scalar.db.analytics.repository.impl.spring.transaction.ConflictTranslatingTransactionManager;
import com.scalar.db.sql.springdata.EnableScalarDbRepositories;
import com.scalar.db.sql.springdata.ScalarDbJdbcConfiguration;
import com.scalar.db.sql.springdata.TimeRelatedTypesConverter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.RollbackOn;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;

@Configuration
@EnableScalarDbRepositories(
    basePackages = "com.scalar.db.analytics.repository.impl.spring.repository.jdbc")
@EnableTransactionManagement(rollbackOn = RollbackOn.ALL_EXCEPTIONS)
// TODO: Remove ScalarDbJdbcConfiguration from @Import after scalardb-sql#1098 is merged.
// Once the default beans have @ConditionalOnMissingBean, AutoConfiguration.imports will handle
// loading without conflicts, making the explicit @Import unnecessary.
@Import({JdbcAuditingConfiguration.class, ScalarDbJdbcConfiguration.class})
@ComponentScan(
    basePackages = {
      "com.scalar.db.analytics.repository.impl.spring.config",
      "com.scalar.db.analytics.repository.impl.spring.repository",
      "com.scalar.db.analytics.repository.impl.spring.query",
      "com.scalar.db.analytics.repository.impl.spring.transaction",
      "com.scalar.db.analytics.repository.impl.spring.exception"
    })
public class SpringDataJdbcConfiguration extends AbstractJdbcConfiguration
    implements TransactionManagementConfigurer {

  private final ObjectMapper objectMapper;
  private final ObjectProvider<PlatformTransactionManager> transactionManager;

  public SpringDataJdbcConfiguration(
      ObjectMapper objectMapper, ObjectProvider<PlatformTransactionManager> transactionManager) {
    this.objectMapper = objectMapper;
    this.transactionManager = transactionManager;
  }

  /**
   * Makes the {@code @Transactional} repository methods report a commit-time transaction conflict
   * as a retryable error. See {@link ConflictTranslatingTransactionManager}. The delegate is the
   * primary transaction manager, the one these methods used before.
   */
  @Override
  public TransactionManager annotationDrivenTransactionManager() {
    return new ConflictTranslatingTransactionManager(transactionManager.getObject());
  }

  @Override
  protected List<?> userConverters() {
    List<Object> converters = new ArrayList<>();
    converters.add(new UuidToStringConverter());
    converters.add(new StringToUuidConverter());
    converters.add(new StringListToJsonConverter(objectMapper));
    converters.add(new JsonToStringListConverter(objectMapper));
    converters.addAll(TimeRelatedTypesConverter.getConvertersToRegister());
    return converters;
  }
}
