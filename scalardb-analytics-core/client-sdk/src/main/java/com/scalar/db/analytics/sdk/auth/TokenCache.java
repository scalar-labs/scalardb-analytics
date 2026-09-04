/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.auth;

import com.scalar.db.analytics.api.auth.AccessToken;
import org.jspecify.annotations.Nullable;

/**
 * Abstraction for persisting and retrieving access tokens across process restarts.
 *
 * <p>Implementations control where and how tokens are cached. This allows short-lived processes
 * (such as CLI invocations) to reuse valid tokens across runs without re-authenticating.
 *
 * <p>The canonical token is always held in-memory by {@link TokenManager}; this interface provides
 * an optional persistence layer.
 */
interface TokenCache {

  /**
   * Persists the given token for the specified username.
   *
   * <p>Implementations should be resilient to I/O errors and must not throw exceptions.
   *
   * @param username the username associated with the token
   * @param token the access token to persist
   */
  void save(String username, AccessToken token);

  /**
   * Loads a previously persisted token for the specified username.
   *
   * <p>Implementations should be resilient to I/O errors and return {@code null} if the token
   * cannot be loaded.
   *
   * @param username the username whose token to load
   * @return the stored access token, or {@code null} if none is available
   */
  @Nullable AccessToken load(String username);
}
