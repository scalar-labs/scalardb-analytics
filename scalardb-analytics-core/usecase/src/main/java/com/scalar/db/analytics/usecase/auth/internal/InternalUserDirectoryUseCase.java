/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase.auth.internal;

import java.util.UUID;

/**
 * Use case for managing credentials in the internal user directory backend.
 *
 * <p>This use case is concerned only with the internal backend's credential records. Principal
 * (AuthUser) management and the link between principal and backend user (PasswordIdentity) are
 * handled by {@code UserUseCase}.
 */
public interface InternalUserDirectoryUseCase {
  /**
   * Creates a credential in the internal user directory. The credential is initially not linked to
   * any principal; use {@code UserUseCase.linkBackendUser} to link it.
   *
   * @param userId the authenticated user ID (must have SUPERADMIN role)
   * @param username the internal-backend username to create
   * @param password the plaintext password
   */
  void createInternalBackendUser(UUID userId, String username, String password);

  /**
   * Deletes a credential from the internal user directory.
   *
   * <p>Without {@code cascade}, fails if the credential is still linked to a principal. With {@code
   * cascade}, the link is removed as part of the operation.
   *
   * @param userId the authenticated user ID (must have SUPERADMIN role)
   * @param username the internal-backend username to delete
   * @param cascade if true, unlink from any principal before deleting
   * @return true if the credential was deleted, false if it was not found
   */
  boolean deleteInternalBackendUser(UUID userId, String username, boolean cascade);
}
