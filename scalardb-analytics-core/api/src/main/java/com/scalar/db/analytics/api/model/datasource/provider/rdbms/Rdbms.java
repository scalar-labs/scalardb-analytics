/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.model.datasource.provider.rdbms;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import java.util.Properties;

public interface Rdbms extends DataSourceProvider {
  @JsonIgnore
  String getUrl();

  @JsonIgnore
  Properties getProperties();

  @JsonIgnore
  String getDriverClassName();
}
