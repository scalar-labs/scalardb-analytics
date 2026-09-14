/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.auth;

import java.util.UUID;
import lombok.Value;

/**
 * Authenticated user identity.
 *
 * <p>Represents a user who has been authenticated. Contains the user ID, which serves as the
 * system-wide identifier linking to tokens, sessions, and authorization decisions, and a
 * human-readable username.
 *
 * <p>How a user authenticates (password, external IdP, etc.) is managed separately by identity
 * records associated with this user ID.
 */
@Value
public class AuthUser {
  UUID userId;
  // TODO: When supporting multiple backends per user, ensure username uniqueness at the
  //  repository level. Currently, the 1:1 user-backend relationship prevents collisions,
  //  but multi-backend support could cause duplicate usernames (e.g., same name from
  //  different backends via JIT provisioning).
  String username;

  public static AuthUser create(String username) {
    if (username.trim().isEmpty()) {
      throw new IllegalArgumentException("username must not be blank");
    }
    return new AuthUser(UUID.randomUUID(), username);
  }
}
