/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase.auth;

import java.util.UUID;

/** Use case for validating access tokens. */
public interface TokenValidationUseCase {
  /**
   * Validates an access token for the given user and returns the associated user ID.
   *
   * @param userId the user ID claiming ownership of the token
   * @param token the access token string to validate
   * @return the user ID associated with the token
   */
  UUID validateToken(UUID userId, String token);
}
