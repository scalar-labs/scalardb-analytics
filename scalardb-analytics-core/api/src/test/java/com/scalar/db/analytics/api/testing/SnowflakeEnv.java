/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.testing;

import com.google.common.base.Strings;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Snowflake;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Value;
import org.jspecify.annotations.Nullable;

/** This provides the environment details of a Snowflake instance used for integration testing */
@Value
public class SnowflakeEnv {
  private static final String PREFIX = DatabricksEnv.ANALYTICS_INT_TEST_ENV + "_SNOWFLAKE_";
  private static final String ACCOUNT = PREFIX + "ACCOUNT";
  private static final String USERNAME = PREFIX + "USERNAME";
  private static final String PASSWORD = PREFIX + "PASSWORD";
  // The database is optional, and the default is null
  private static final String DATABASE = PREFIX + "DATABASE";
  // The database suffix is optional, and the default is the current unix timestamp
  // It is used to create a unique database for each test run
  private static final String DATABASE_SUFFIX = PREFIX + "DATABASE_SUFFIX";

  String account;
  String username;
  String password;
  @Nullable String database;
  String databaseSuffix;
  HikariDataSource dataSource;
  Snowflake snowflake;

  public SnowflakeEnv() {
    account = getRequiredSystemProperty(ACCOUNT);
    username = getRequiredSystemProperty(USERNAME);
    password = getRequiredSystemProperty(PASSWORD);
    database = System.getenv().getOrDefault(DATABASE, null);

    String defaultDatabaseSuffix = String.valueOf(System.currentTimeMillis());
    databaseSuffix = System.getProperty(DATABASE_SUFFIX, defaultDatabaseSuffix);

    snowflake = new Snowflake(account, username, password, database);
    dataSource = new HikariDataSource();
    dataSource.setDriverClassName(snowflake.getDriverClassName());
    dataSource.setJdbcUrl(snowflake.getUrl());
    dataSource.setDataSourceProperties(snowflake.getProperties());
  }

  private String getRequiredSystemProperty(String key) {
    String value = System.getenv(key);
    if (Strings.isNullOrEmpty(value)) {
      throw new IllegalArgumentException(
          "Snowflake integration test environment variable '" + key + "' is not set");
    }
    return value;
  }
}
