/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.codec.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Snowflake;

public class SnowflakeCodec extends JacksonizedProviderCodec<Snowflake> {

  public SnowflakeCodec(ObjectMapper objectMapper) {
    super(Snowflake.class, Snowflake.TYPE, objectMapper);
  }
}
