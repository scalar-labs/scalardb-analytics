/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase.auth;

import com.scalar.db.analytics.api.auth.AuthUser;
import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.api.auth.UserDetail;
import com.scalar.db.analytics.api.auth.UserInfo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Use case for Analytics user (principal) management and the principal ↔ backend-user link.
 *
 * <p>Roles and permissions attach to principals. Backend-user credentials (e.g., the internal
 * backend) are managed by their own use case (e.g., {@code InternalUserDirectoryUseCase}). Linking
 * the two is the responsibility of this use case via {@code linkBackendUser} / {@code
 * unlinkBackendUser}.
 */
public interface UserUseCase {
  // --- Read ---

  /** Lists all users regardless of authentication backend. SUPERADMIN only. */
  List<UserInfo> listUsers(UUID userId);

  /** Describes a user by ID, including assigned roles. SUPERADMIN only. */
  Optional<UserDetail> describeUserById(UUID userId, UUID targetUserId);

  /** Describes a user by username, including assigned roles. SUPERADMIN only. */
  Optional<UserDetail> describeUserByName(UUID userId, String username);

  // --- Write: principal ---

  /**
   * Creates an Analytics user (principal) with no credential attached.
   *
   * @param userId the authenticated user ID (SUPERADMIN required)
   * @param username the name of the principal to create
   * @return the created principal
   */
  AuthUser createUser(UUID userId, String username);

  /**
   * Deletes a principal by name. Without {@code cascade}, fails if linked backend users,
   * identities, or role assignments still exist.
   *
   * @return true if deleted, false if the principal was not found
   */
  boolean deleteUser(UUID userId, String username, boolean cascade);

  /** Deletes a principal by ID. See {@link #deleteUser(UUID, String, boolean)}. */
  boolean deleteUserById(UUID userId, UUID targetUserId, boolean cascade);

  /**
   * Convenience composite: atomically creates a principal, creates an internal-backend user, and
   * links them.
   *
   * @param userId the authenticated user ID (SUPERADMIN required)
   * @param username the name of the principal to create
   * @param backendUsername the name of the internal-backend user to create and link
   * @param password the plaintext password for the backend user
   * @return the created principal
   */
  AuthUser createUserWithBackendUser(
      UUID userId, String username, String backendUsername, String password);

  // --- Write: link ---

  /**
   * Links an existing principal to an existing backend user.
   *
   * @param userId the authenticated user ID (SUPERADMIN required)
   * @param username the principal name
   * @param backendUsername the backend user name
   * @param backend the user-directory backend
   */
  void linkBackendUser(
      UUID userId, String username, String backendUsername, PasswordBackendType backend);

  /** Removes the link between a principal and a backend user. */
  void unlinkBackendUser(
      UUID userId, String username, String backendUsername, PasswordBackendType backend);
}
