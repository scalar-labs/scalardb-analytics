/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase.auth;

import com.scalar.db.analytics.api.auth.PasswordBackendType;

/**
 * Strategy interface for password authentication backends.
 *
 * <p>Implementations verify credentials against specific backends (internal database, ScalarDB
 * Cluster, etc.) and return the backend user ID and optional auth token upon successful
 * verification.
 */
public interface PasswordAuthenticationBackend {

  /**
   * Verifies the provided credentials.
   *
   * @param username the username to verify
   * @param password the password to verify
   * @return the authentication result containing the backend user ID and optional backend token
   */
  BackendAuthResult verifyCredential(String username, String password);

  /**
   * Returns the backend type this implementation represents.
   *
   * <p>Used by callers that need to scope identity lookups and deletes to a specific backend (e.g.
   * {@code PasswordIdentityRepository.findByBackendAndBackendUserId}).
   */
  PasswordBackendType getBackendType();
}
