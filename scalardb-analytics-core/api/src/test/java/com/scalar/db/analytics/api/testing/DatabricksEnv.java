/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.testing;

import com.google.common.base.Strings;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Databricks;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Value;
import org.jspecify.annotations.Nullable;

/** This provides the environment details of a Databricks instance used for integration testing */
@Value
public class DatabricksEnv {
  public static final String ANALYTICS_INT_TEST_ENV = "ANALYTICS_INT_TEST_ENV";
  private static final String PREFIX = ANALYTICS_INT_TEST_ENV + "_DATABRICKS_";
  private static final String HOST = PREFIX + "HOST";
  private static final String HTTP_PATH = PREFIX + "HTTP_PATH";
  // The port is optional, and the default is 443
  private static final String PORT = PREFIX + "PORT";
  private static final String OAUTH_CLIENT_ID = PREFIX + "O_AUTH_CLIENT_ID";
  private static final String OAUTH_CLIENT_SECRET = PREFIX + "O_AUTH_SECRET";
  // The catalog is optional, and the default is null
  private static final String CATALOG = PREFIX + "CATALOG";
  // The catalog suffix is optional, and the default is the current unix timestamp
  // It is used to create unique catalog names for each test run
  private static final String CATALOG_SUFFIX = PREFIX + "CATALOG_SUFFIX";

  private static final String DEFAULT_PORT = "443";

  String host;
  String port;
  String httpPath;
  String oAuthClientId;
  String oAuthClientSecret;
  String catalogSuffix;
  @Nullable String catalog;
  Databricks databricks;

  HikariDataSource dataSource;

  public DatabricksEnv() {
    host = getSystemProperty(HOST, null);
    port = getSystemProperty(PORT, DEFAULT_PORT);
    httpPath = getSystemProperty(HTTP_PATH, null);
    oAuthClientId = getSystemProperty(OAUTH_CLIENT_ID, null);
    oAuthClientSecret = getSystemProperty(OAUTH_CLIENT_SECRET, null);
    catalog = System.getenv().getOrDefault(CATALOG, null);

    String defaultCatalogSuffix = String.valueOf(System.currentTimeMillis());
    catalogSuffix = System.getProperty(CATALOG_SUFFIX, defaultCatalogSuffix);

    databricks =
        new Databricks(
            host, Integer.parseInt(port), httpPath, oAuthClientId, oAuthClientSecret, catalog);
    dataSource = new HikariDataSource();
    dataSource.setDriverClassName(databricks.getDriverClassName());
    dataSource.setJdbcUrl(databricks.getUrl());
    dataSource.setDataSourceProperties(databricks.getProperties());
  }

  private String getSystemProperty(String key, @Nullable String defaultValue) {
    String value = System.getenv().getOrDefault(key, defaultValue);
    if (Strings.isNullOrEmpty(value)) {
      throw new IllegalArgumentException(
          "Databricks integration test environment variable '" + key + "' is not set");
    }
    return value;
  }
}
