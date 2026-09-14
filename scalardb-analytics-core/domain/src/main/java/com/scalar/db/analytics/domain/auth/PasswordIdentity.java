/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.domain.auth;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import java.util.UUID;

/**
 * Links an authentication backend identity to an internal user.
 *
 * <p>A user may authenticate through multiple backends (e.g., internal password, ScalarDB Cluster).
 * Each backend identifies the user differently (username, email, etc.). This record maps those
 * external identities to the internal {@code userId} used throughout the system.
 *
 * @param identityId unique identifier for this identity mapping
 * @param userId the internal user ID this identity belongs to
 * @param backend the authentication backend type
 * @param backendUserId the user identifier within that backend (e.g., username, email)
 */
public record PasswordIdentity(
    UUID identityId, UUID userId, PasswordBackendType backend, String backendUserId) {

  public static PasswordIdentity create(
      UUID userId, PasswordBackendType backend, String backendUserId) {
    return new PasswordIdentity(UUID.randomUUID(), userId, backend, backendUserId);
  }
}
