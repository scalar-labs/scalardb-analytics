/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.codec.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Oracle;

public class OracleCodec extends JacksonizedProviderCodec<Oracle> {

  public OracleCodec(ObjectMapper objectMapper) {
    super(Oracle.class, Oracle.TYPE, objectMapper);
  }
}
