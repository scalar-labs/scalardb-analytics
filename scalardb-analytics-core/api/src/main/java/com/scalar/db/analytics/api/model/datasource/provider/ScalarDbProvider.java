/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.model.datasource.provider;

import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import com.scalar.db.analytics.api.model.datasource.DataSourceProviderVisitor;
import java.util.Map;
import java.util.Properties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder
@AllArgsConstructor
public class ScalarDbProvider implements DataSourceProvider {
  public static final String TYPE = "scalardb";

  Map<String, String> configs;

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public <T> T accept(DataSourceProviderVisitor<T> visitor) {
    return visitor.visit(this);
  }

  /**
   * Converts the configuration map to a Properties object.
   *
   * @return a Properties object containing all configuration entries
   */
  public Properties toProperties() {
    Properties properties = new Properties();
    properties.putAll(configs);
    return properties;
  }
}
