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

@Value
@Jacksonized
@Builder
@AllArgsConstructor
public class PostgreSql implements Rdbms {
  public static final String TYPE = "postgresql";

  String host;
  int port;
  String username;
  String password;
  String database;

  @Override
  public String getUrl() {
    return "jdbc:postgresql://" + host + ":" + port + "/" + database;
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
    return properties;
  }

  @Override
  public String getDriverClassName() {
    return "org.postgresql.Driver";
  }
}
