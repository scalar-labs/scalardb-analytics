/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.auth;

import com.scalar.db.analytics.usecase.auth.BackendAuthResult;
import com.scalar.db.analytics.usecase.auth.BackendTokenStore;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** {@link ConcurrentHashMap}-backed implementation of {@link BackendTokenStore}. */
public class InMemoryBackendTokenStore implements BackendTokenStore {

  private final ConcurrentMap<UUID, BackendAuthResult> entries = new ConcurrentHashMap<>();

  @Override
  public void store(UUID userId, BackendAuthResult authResult) {
    entries.put(userId, authResult);
  }

  @Override
  public Optional<BackendAuthResult> get(UUID userId) {
    return Optional.ofNullable(entries.get(userId));
  }

  @Override
  public void remove(UUID userId) {
    entries.remove(userId);
  }
}
