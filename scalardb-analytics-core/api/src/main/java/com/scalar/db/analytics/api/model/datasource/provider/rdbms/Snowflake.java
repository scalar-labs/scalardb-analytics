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
public class Snowflake implements Rdbms {
  public static final String TYPE = "snowflake";

  String account;
  String username;
  String password;
  @Nullable String database;

  public Snowflake(String account, String username, String password) {
    this(account, username, password, null);
  }

  @Override
  public String getUrl() {
    return "jdbc:snowflake://" + account + ".snowflakecomputing.com/";
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
      properties.put("db", database);
    }
    return properties;
  }

  @Override
  public String getDriverClassName() {
    return "net.snowflake.client.jdbc.SnowflakeDriver";
  }
}
