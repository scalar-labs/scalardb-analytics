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
import com.scalar.db.sql.springdata.EnableScalarDbRepositories;
import com.scalar.db.sql.springdata.ScalarDbJdbcConfiguration;
import com.scalar.db.sql.springdata.TimeRelatedTypesConverter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.RollbackOn;

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
public class SpringDataJdbcConfiguration extends AbstractJdbcConfiguration {

  private final ObjectMapper objectMapper;

  public SpringDataJdbcConfiguration(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
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
