/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.codec.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.SqlServer;

public class SqlServerCodec extends JacksonizedProviderCodec<SqlServer> {

  public SqlServerCodec(ObjectMapper objectMapper) {
    super(SqlServer.class, SqlServer.TYPE, objectMapper);
  }
}
