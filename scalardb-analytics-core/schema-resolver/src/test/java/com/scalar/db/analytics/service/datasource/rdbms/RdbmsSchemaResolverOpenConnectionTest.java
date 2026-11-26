/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.analytics.service.datasource.InvalidDataSourceConfigurationException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RdbmsSchemaResolverOpenConnectionTest {

  private static final String IAE_URL = "jdbc:analytics-test-iae:db";
  private static final String SQL_EXCEPTION_URL = "jdbc:analytics-test-sql:db";
  private static final TestDriver DRIVER = new TestDriver();

  @BeforeAll
  static void registerDriver() throws SQLException {
    DriverManager.registerDriver(DRIVER);
  }

  @AfterAll
  static void deregisterDriver() throws SQLException {
    DriverManager.deregisterDriver(DRIVER);
  }

  @Test
  void open_whenDriverThrowsIllegalArgumentException_shouldThrowInvalidConfiguration() {
    assertThatThrownBy(() -> open(IAE_URL))
        .isInstanceOf(InvalidDataSourceConfigurationException.class)
        .hasCauseInstanceOf(NumberFormatException.class);
  }

  @Test
  void open_whenDriverThrowsSqlException_shouldThrowJdbcException() {
    assertThatThrownBy(() -> open(SQL_EXCEPTION_URL))
        .isInstanceOf(JdbcException.class)
        .isNotInstanceOf(InvalidDataSourceConfigurationException.class);
  }

  private static void open(String url) {
    SchemaResolverMySql.open(
        UUID.randomUUID(), url, new Properties(), RdbmsSchemaResolverOption.DEFAULT, null);
  }

  /**
   * A driver that fails the way real drivers do for a malformed URL: the Databricks driver throws
   * NumberFormatException for a non-numeric port, while most others throw SQLException.
   */
  private static final class TestDriver implements Driver {
    @Override
    public @Nullable Connection connect(String url, Properties info) throws SQLException {
      if (url.startsWith(IAE_URL)) {
        throw new NumberFormatException("For input string: \"abc\"");
      }
      if (url.startsWith(SQL_EXCEPTION_URL)) {
        throw new SQLException("Incorrect port value");
      }
      // A driver returns null for a URL it does not handle.
      return null;
    }

    @Override
    public boolean acceptsURL(String url) {
      return url.startsWith(IAE_URL) || url.startsWith(SQL_EXCEPTION_URL);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
      return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
      return 1;
    }

    @Override
    public int getMinorVersion() {
      return 0;
    }

    @Override
    public boolean jdbcCompliant() {
      return false;
    }

    @Override
    public Logger getParentLogger() {
      return Logger.getGlobal();
    }
  }
}
