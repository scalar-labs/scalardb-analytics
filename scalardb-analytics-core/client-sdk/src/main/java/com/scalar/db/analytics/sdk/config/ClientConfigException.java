/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.config;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import java.util.HashMap;
import java.util.Map;

/**
 * Thrown when the resolved client configuration is incomplete, for example when a username is set
 * but no password is set in any source.
 *
 * <p>The error code is {@link AnalyticsErrorCode#INVALID_ARGUMENT}, with the missing key's {@link
 * ClientConfigKey#propertyKey()} in the {@code field} metadata and its {@link
 * ClientConfigKey#envVarName()} in the {@code env_var} metadata. Integrations can use {@link
 * #key()} to report the key under their own name.
 */
public class ClientConfigException extends AnalyticsException {
  private static final long serialVersionUID = 1L;

  private final ClientConfigKey key;

  ClientConfigException(ClientConfigKey key) {
    super(AnalyticsErrorCode.INVALID_ARGUMENT, metadata(key));
    this.key = key;
  }

  /** Returns the key that is required but not set. */
  public ClientConfigKey key() {
    return key;
  }

  private static Map<String, String> metadata(ClientConfigKey key) {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("field", key.propertyKey());
    metadata.put("env_var", key.envVarName());
    metadata.put("reason", "required");
    return metadata;
  }
}
