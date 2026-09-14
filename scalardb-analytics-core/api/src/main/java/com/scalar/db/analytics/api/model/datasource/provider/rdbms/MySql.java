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
public class MySql implements Rdbms {
  public static final String TYPE = "mysql";

  private static final String DEFAULT_SSL_MODE = "trust";

  String host;
  int port;
  String username;
  String password;

  // `database` is optional for MySql because it is not required to connect to the MySql server.
  // If `database` is provided, only the tables in that database will be imported.
  // The specified `database` will also be used as the default database for the connection.
  // If not provided, all the tables in the server will be imported.
  @Nullable String database;

  // `sslMode` is optional and maps to the MariaDB Connector/J `sslMode` connection option
  // (`disable` / `trust` / `verify-ca` / `verify-full`). It is passed through to the driver as
  // given, so the driver is the single source of truth for the accepted values.
  //
  // When not specified, `trust` is used instead of the driver default `disable`. MySQL 8.0.4 and
  // later default every account to the `caching_sha2_password` plugin, whose full authentication
  // needs either an encrypted channel or RSA public key retrieval to transmit the password. Since
  // MySQL 8.0 enables TLS by default, `trust` keeps those connections working and matches the
  // protection level of the `sslMode=PREFERRED` default of the MySQL Connector/J this driver
  // replaced. Set `disable` explicitly for servers with TLS turned off.
  @Nullable String sslMode;

  public MySql(String host, int port, String username, String password) {
    this(host, port, username, password, null, null);
  }

  public MySql(String host, int port, String username, String password, @Nullable String database) {
    this(host, port, username, password, database, null);
  }

  @Override
  public String getUrl() {
    return "jdbc:mysql://"
        + host
        + ":"
        + port
        + "/"
        + (database == null ? "" : database)
        + "?permitMysqlScheme=true"
        + "&sslMode="
        + (sslMode == null ? DEFAULT_SSL_MODE : sslMode);
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
    return "org.mariadb.jdbc.Driver";
  }
}
