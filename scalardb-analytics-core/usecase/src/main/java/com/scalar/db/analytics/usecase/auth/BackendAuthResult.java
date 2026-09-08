/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase.auth;

import org.jspecify.annotations.Nullable;

/**
 * Result of a backend credential verification containing the backend user ID and an optional
 * backend-issued auth token.
 *
 * @param backendUserId the user ID on the backend system (typically the username)
 * @param backendToken the auth token issued by the backend, or {@code null} if the backend does not
 *     issue tokens
 */
public record BackendAuthResult(String backendUserId, @Nullable String backendToken) {

  /** Creates a result with no backend token. */
  public static BackendAuthResult of(String backendUserId) {
    return new BackendAuthResult(backendUserId, null);
  }
}
