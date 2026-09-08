/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.model;

import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import java.util.UUID;
import lombok.Value;

@Value
public class DataSource {
  UUID id;
  UUID catalogId;
  String name;
  DataSourceProvider provider;

  public static DataSource create(UUID catalogId, String name, DataSourceProvider provider) {
    return new DataSource(UUID.randomUUID(), catalogId, name, provider);
  }
}
