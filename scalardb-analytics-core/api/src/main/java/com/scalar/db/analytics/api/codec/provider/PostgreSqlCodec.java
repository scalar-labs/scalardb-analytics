/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.codec.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;

public class PostgreSqlCodec extends JacksonizedProviderCodec<PostgreSql> {

  public PostgreSqlCodec(ObjectMapper objectMapper) {
    super(PostgreSql.class, PostgreSql.TYPE, objectMapper);
  }
}
