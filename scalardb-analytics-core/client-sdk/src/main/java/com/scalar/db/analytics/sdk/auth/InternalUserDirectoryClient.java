/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.auth;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.api.error.AnalyticsException;

/**
 * Client for managing credentials in the internal authentication backend.
 *
 * <p>Principal (Analytics user) management and the principal ↔ backend-user link are on {@link
 * UserClient}.
 */
public interface InternalUserDirectoryClient {

  /**
   * Creates a credential in the internal user directory. The credential is initially not linked to
   * any principal; use {@link UserClient#linkBackendUser(String, String, PasswordBackendType)} to
   * link it.
   *
   * @param username the internal-backend username
   * @param password the plaintext password
   * @throws AnalyticsException if the operation fails
   */
  void createInternalBackendUser(String username, String password);

  /**
   * Deletes a credential from the internal user directory. Without {@code cascade}, fails if the
   * credential is still linked to a principal.
   *
   * @param username the internal-backend username
   * @param cascade if true, unlink from any principal before deleting
   * @return {@code true} if the credential was deleted, {@code false} if it was not found
   * @throws AnalyticsException if the operation fails
   */
  boolean deleteInternalBackendUser(String username, boolean cascade);
}
