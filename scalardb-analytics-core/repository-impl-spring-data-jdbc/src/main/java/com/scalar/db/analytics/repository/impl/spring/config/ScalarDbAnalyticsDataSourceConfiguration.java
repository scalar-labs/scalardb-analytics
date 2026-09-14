/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.config;

import com.scalar.db.analytics.repository.impl.spring.autoconfigure.ScalarDbSqlProperties;
import java.util.Map;
import java.util.StringJoiner;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
@EnableConfigurationProperties(ScalarDbSqlProperties.class)
public class ScalarDbAnalyticsDataSourceConfiguration {

  private static final String SCALAR_DB_DRIVER = "com.scalar.db.sql.jdbc.SqlJdbcDriver";

  private final ScalarDbSqlProperties properties;

  public ScalarDbAnalyticsDataSourceConfiguration(ScalarDbSqlProperties properties) {
    this.properties = properties;
  }

  @Bean
  @ConditionalOnMissingBean
  public DataSource dataSource() {
    String jdbcUrl = buildJdbcUrl();

    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    // DriverManagerDataSource is acceptable here because ScalarDB SQL direct mode keeps
    // connections in-process and provides its own pooling semantics.
    dataSource.setDriverClassName(SCALAR_DB_DRIVER);
    dataSource.setUrl(jdbcUrl);
    return dataSource;
  }

  private String buildJdbcUrl() {
    Map<String, String> params = properties.toMapWithDefaults();

    StringJoiner joiner = new StringJoiner("&");
    params.forEach((key, value) -> joiner.add(key + "=" + value));
    return "jdbc:scalardb:" + "?" + joiner;
  }
}
