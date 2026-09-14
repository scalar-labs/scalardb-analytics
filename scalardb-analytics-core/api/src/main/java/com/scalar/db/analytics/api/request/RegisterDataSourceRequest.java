/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.request;

import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import com.scalar.db.analytics.api.request.schema.DataSourceSchema;
import lombok.Value;
import org.jspecify.annotations.Nullable;

@Value
public class RegisterDataSourceRequest {
  String catalogName;
  String name;
  DataSourceProvider provider;
  @Nullable DataSourceSchema schema;

  public String getProviderType() {
    return provider.getType();
  }
}
