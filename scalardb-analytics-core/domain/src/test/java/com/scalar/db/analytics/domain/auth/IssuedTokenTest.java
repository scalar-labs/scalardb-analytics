/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IssuedTokenTest {

  @Test
  void issueShouldGenerateTokenWithCorrectFields() {
    UUID userId = UUID.randomUUID();
    Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);

    IssuedToken token = IssuedToken.issue(userId, expiresAt);

    assertThat(token.token()).isNotNull().isNotEmpty();
    assertThat(token.userId()).isEqualTo(userId);
    assertThat(token.expiresAt()).isEqualTo(expiresAt);
  }

  @Test
  void issueShouldGenerateUniqueTokens() {
    UUID userId = UUID.randomUUID();
    Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);

    Set<String> tokens = new HashSet<>();
    for (int i = 0; i < 100; i++) {
      IssuedToken token = IssuedToken.issue(userId, expiresAt);
      tokens.add(token.token());
    }

    assertThat(tokens).hasSize(100);
  }

  @Test
  void issueShouldGenerateBase64UrlEncodedToken() {
    UUID userId = UUID.randomUUID();
    Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);

    IssuedToken token = IssuedToken.issue(userId, expiresAt);

    // Base64 URL-safe characters: A-Z, a-z, 0-9, -, _
    assertThat(token.token()).matches("^[A-Za-z0-9_-]+$");
    // 32 bytes encoded in Base64 = 43 characters (without padding)
    assertThat(token.token()).hasSize(43);
  }

  @Test
  void isExpiredAtShouldReturnFalseBeforeExpiration() {
    UUID userId = UUID.randomUUID();
    Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
    IssuedToken token = new IssuedToken("test-token", userId, expiresAt);

    assertThat(token.isExpiredAt(Instant.now())).isFalse();
  }

  @Test
  void isExpiredAtShouldReturnTrueAfterExpiration() {
    UUID userId = UUID.randomUUID();
    Instant expiresAt = Instant.now().minus(1, ChronoUnit.HOURS);
    IssuedToken token = new IssuedToken("test-token", userId, expiresAt);

    assertThat(token.isExpiredAt(Instant.now())).isTrue();
  }

  @Test
  void isExpiredAtShouldReturnTrueAtExactExpiration() {
    UUID userId = UUID.randomUUID();
    Instant expiresAt = Instant.now();
    IssuedToken token = new IssuedToken("test-token", userId, expiresAt);

    // isAfter returns false for equal times, so token is not expired at exact time
    assertThat(token.isExpiredAt(expiresAt)).isFalse();
    // But it's expired one nanosecond after
    assertThat(token.isExpiredAt(expiresAt.plusNanos(1))).isTrue();
  }
}
