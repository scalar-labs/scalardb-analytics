/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase.auth;

import java.util.Optional;
import java.util.UUID;

/**
 * In-memory store for backend authentication results.
 *
 * <p>When an external authentication backend (e.g., ScalarDB Cluster) returns an auth result during
 * login, the result is stored here keyed by the Analytics user ID. Downstream components can
 * retrieve the result to make authenticated calls to the backend.
 */
public interface BackendTokenStore {

  /**
   * Stores a backend authentication result for the given user, replacing any existing entry.
   *
   * @param userId the Analytics user ID
   * @param authResult the backend authentication result
   */
  void store(UUID userId, BackendAuthResult authResult);

  /**
   * Retrieves the backend authentication result for the given user.
   *
   * @param userId the Analytics user ID
   * @return the result, or empty if no entry is stored for this user
   */
  Optional<BackendAuthResult> get(UUID userId);

  /**
   * Removes the backend authentication result for the given user.
   *
   * @param userId the Analytics user ID
   */
  void remove(UUID userId);
}
