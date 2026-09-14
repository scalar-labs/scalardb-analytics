/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.model.datasource.provider.rdbms;

import com.scalar.db.analytics.api.model.datasource.DataSourceProviderVisitor;
import java.util.Properties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.jspecify.annotations.Nullable;

@Value
@Jacksonized
@Builder
@AllArgsConstructor
public class SqlServer implements Rdbms {
  public static final String TYPE = "sqlserver";

  String host;
  int port;
  String username;
  String password;

  @Nullable String database;
  @Nullable Boolean secure;

  public SqlServer(
      String host, int port, String username, String password, @Nullable String database) {
    this(host, port, username, password, database, null);
  }

  @Override
  public String getUrl() {
    return "jdbc:sqlserver://" + host + ":" + port;
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public <T> T accept(DataSourceProviderVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public Properties getProperties() {
    Properties properties = new Properties();
    properties.put("user", username);
    properties.put("password", password);

    if (database != null) {
      properties.put("databaseName", database);
    }

    if (secure != null) {
      properties.put("encrypt", secure ? "true" : "false");
    }

    return properties;
  }

  @Override
  public String getDriverClassName() {
    return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
  }
}
