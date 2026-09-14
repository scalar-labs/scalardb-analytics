/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.auth;

import com.scalar.db.analytics.api.auth.AccessToken;
import org.jspecify.annotations.Nullable;

/**
 * A no-op {@link TokenCache} that does not persist tokens.
 *
 * <p>This is the default implementation used when no token cache directory is configured. Token
 * lifecycle is managed entirely in-memory by {@link TokenManager}.
 */
class NoOpTokenCache implements TokenCache {

  @Override
  public void save(String username, AccessToken token) {
    // No-op: tokens are not persisted
  }

  @Nullable
  @Override
  public AccessToken load(String username) {
    return null;
  }
}
