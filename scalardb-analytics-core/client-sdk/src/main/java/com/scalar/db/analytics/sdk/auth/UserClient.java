/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.auth;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.api.auth.UserDetail;
import com.scalar.db.analytics.api.auth.UserInfo;
import com.scalar.db.analytics.api.error.AnalyticsException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Client for Analytics user (principal) management and the principal ↔ backend-user link. */
public interface UserClient {

  // --- Read ---

  /** Lists all users regardless of authentication backend. */
  List<UserInfo> listUsers();

  /** Describes a user by ID, including assigned roles. */
  Optional<UserDetail> describeUserById(UUID userId);

  /** Describes a user by username, including assigned roles. */
  Optional<UserDetail> describeUserByName(String username);

  // --- Write: principal ---

  /**
   * Creates an Analytics user (principal) with no credential attached.
   *
   * @param username the principal name
   * @return the new principal's user ID
   * @throws AnalyticsException if the operation fails
   */
  String createUser(String username);

  /**
   * Deletes a principal by name. Without {@code cascade}, fails if dependents exist.
   *
   * @return {@code true} if deleted, {@code false} if not found
   */
  boolean deleteUser(String username, boolean cascade);

  /** Deletes a principal by ID. See {@link #deleteUser(String, boolean)}. */
  boolean deleteUserById(UUID userId, boolean cascade);

  /**
   * Convenience composite: atomically creates a principal, creates an internal-backend user, and
   * links them.
   *
   * @return the new principal's user ID
   */
  String createUserWithBackendUser(String username, String backendUsername, String password);

  // --- Write: link ---

  /**
   * Links an existing principal to an existing backend user.
   *
   * @param backend the user-directory backend
   */
  void linkBackendUser(String username, String backendUsername, PasswordBackendType backend);

  /** Removes the link between a principal and a backend user. */
  void unlinkBackendUser(String username, String backendUsername, PasswordBackendType backend);
}
