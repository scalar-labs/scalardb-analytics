/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase.auth;

import com.scalar.db.analytics.api.auth.AccessToken;
import com.scalar.db.analytics.api.auth.PasswordCredential;

/** Use case for authentication operations. */
public interface AuthenticationUseCase {
  /**
   * Authenticates a user with password credential.
   *
   * @param credential the password credential containing username and password
   * @return the access token containing the token string and expiration time
   */
  AccessToken authenticateWithPassword(PasswordCredential credential);
}
