/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.model.datasource.provider.rdbms;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MySqlTest {

  @Test
  void getUrl_whenSslModeNotSpecified_shouldDefaultToTrust() {
    MySql mySql = new MySql("localhost", 3306, "user", "pass", "testdb");

    assertThat(mySql.getUrl())
        .isEqualTo("jdbc:mysql://localhost:3306/testdb?permitMysqlScheme=true&sslMode=trust");
  }

  @Test
  void getUrl_whenSslModeSpecified_shouldUseIt() {
    MySql mySql = new MySql("localhost", 3306, "user", "pass", "testdb", "disable");

    assertThat(mySql.getUrl())
        .isEqualTo("jdbc:mysql://localhost:3306/testdb?permitMysqlScheme=true&sslMode=disable");
  }

  @Test
  void getUrl_whenDatabaseNotSpecified_shouldOmitDatabase() {
    MySql mySql = new MySql("localhost", 3306, "user", "pass");

    assertThat(mySql.getUrl())
        .isEqualTo("jdbc:mysql://localhost:3306/?permitMysqlScheme=true&sslMode=trust");
  }
}
