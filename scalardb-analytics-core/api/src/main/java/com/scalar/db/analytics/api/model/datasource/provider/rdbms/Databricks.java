/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.model.datasource.provider.rdbms;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.scalar.db.analytics.api.model.datasource.DataSourceProviderVisitor;
import java.util.Properties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.jspecify.annotations.Nullable;

@Value
@Jacksonized
@Builder
@AllArgsConstructor
public class Databricks implements Rdbms {
  public static final String TYPE = "databricks";

  String host;
  @Nullable Integer port;
  String httpPath;

  @Getter(onMethod_ = @JsonProperty("oAuthClientId"))
  String oAuthClientId;

  @Getter(onMethod_ = @JsonProperty("oAuthSecret"))
  String oAuthSecret;

  @Nullable String catalog;

  public Databricks(String host, String httpPath, String oAuthClientId, String oAuthSecret) {
    this(host, null, httpPath, oAuthClientId, oAuthSecret, null);
  }

  @Override
  public String getUrl() {
    String url = "jdbc:databricks://" + host;
    if (port != null) {
      url += ":" + port;
    }

    return url;
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
    properties.put("httpPath", httpPath);
    properties.put("AuthMech", "11");
    properties.put("Auth_Flow", "1");
    properties.put("OAuth2ClientID", oAuthClientId);
    properties.put("OAuth2Secret", oAuthSecret);
    if (catalog != null) {
      properties.put("catalog", catalog);
    }
    return properties;
  }

  @Override
  public String getDriverClassName() {
    return "com.databricks.client.jdbc.Driver";
  }
}
