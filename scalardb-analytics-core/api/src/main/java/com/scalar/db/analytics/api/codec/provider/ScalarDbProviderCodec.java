/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.codec.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider;

public class ScalarDbProviderCodec extends JacksonizedProviderCodec<ScalarDbProvider> {

  public ScalarDbProviderCodec(ObjectMapper objectMapper) {
    super(ScalarDbProvider.class, ScalarDbProvider.TYPE, objectMapper);
  }
}
