/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.codec.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.api.model.datasource.provider.DynamoDbProvider;

public class DynamoDbProviderCodec extends JacksonizedProviderCodec<DynamoDbProvider> {

  public DynamoDbProviderCodec(ObjectMapper objectMapper) {
    super(DynamoDbProvider.class, DynamoDbProvider.TYPE, objectMapper);
  }
}
