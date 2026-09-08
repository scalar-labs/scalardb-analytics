/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.codec.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Databricks;

public class DatabricksCodec extends JacksonizedProviderCodec<Databricks> {

  public DatabricksCodec(ObjectMapper objectMapper) {
    super(Databricks.class, Databricks.TYPE, objectMapper);
  }
}
