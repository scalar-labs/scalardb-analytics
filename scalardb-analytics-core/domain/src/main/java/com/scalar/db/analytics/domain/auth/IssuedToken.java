/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.domain.auth;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Server-side record of an issued access token.
 *
 * <p>Represents a token that has been issued to a user after successful authentication. This domain
 * model is stored in the database and used to validate incoming requests. The {@code token} field
 * is the primary identifier and contains the opaque token string that clients present in their
 * requests.
 *
 * @param token the opaque token string issued to the client (primary identifier)
 * @param userId the user to whom this token was issued
 * @param expiresAt when this token expires and becomes invalid
 */
public record IssuedToken(String token, UUID userId, Instant expiresAt) {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final int TOKEN_BYTES = 32;

  /**
   * Issues a new access token for a user.
   *
   * <p>Generates a cryptographically secure random token string using {@link SecureRandom}.
   *
   * @param userId the user to whom this token is issued
   * @param expiresAt when this token should expire
   * @return a new IssuedToken with a randomly generated token string
   */
  public static IssuedToken issue(UUID userId, Instant expiresAt) {
    byte[] bytes = new byte[TOKEN_BYTES];
    SECURE_RANDOM.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    return new IssuedToken(token, userId, expiresAt);
  }

  public boolean isExpiredAt(Instant time) {
    return time.isAfter(expiresAt);
  }
}
