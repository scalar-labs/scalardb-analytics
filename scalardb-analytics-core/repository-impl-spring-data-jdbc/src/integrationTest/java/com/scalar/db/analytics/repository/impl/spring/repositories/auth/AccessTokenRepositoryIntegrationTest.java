/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repositories.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.scalar.db.analytics.api.auth.AuthUser;
import com.scalar.db.analytics.domain.auth.IssuedToken;
import com.scalar.db.analytics.repository.impl.spring.repository.auth.AccessTokenRepositoryImpl;
import com.scalar.db.analytics.repository.impl.spring.repository.auth.AuthUserRepositoryImpl;
import com.scalar.db.analytics.repository.impl.spring.support.AbstractScalarDbIntegrationTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AccessTokenRepositoryIntegrationTest extends AbstractScalarDbIntegrationTest {

  @Autowired private AuthUserRepositoryImpl authUserRepository;
  @Autowired private AccessTokenRepositoryImpl accessTokenRepository;

  private AuthUser testUser;

  @BeforeEach
  void setUpUser() {
    testUser = AuthUser.create("testuser");
    authUserRepository.create(ctx, testUser);
  }

  @Test
  void createShouldPersistToken() {
    Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);
    IssuedToken token = new IssuedToken("test-token-value", testUser.getUserId(), expiresAt);

    accessTokenRepository.create(ctx, token);

    Optional<IssuedToken> found =
        accessTokenRepository.findByUserIdAndToken(ctx, testUser.getUserId(), "test-token-value");
    assertThat(found).isPresent();
    assertThat(found.get().userId()).isEqualTo(testUser.getUserId());
    assertThat(found.get().expiresAt()).isEqualTo(expiresAt);
  }

  @Test
  void findByUserIdAndTokenShouldReturnEmptyWhenMissing() {
    assertThat(
            accessTokenRepository.findByUserIdAndToken(
                ctx, testUser.getUserId(), "nonexistent-token"))
        .isEmpty();
  }

  @Test
  void deleteByUserIdShouldRemoveTokenForUser() {
    Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);
    IssuedToken token = new IssuedToken("token1", testUser.getUserId(), expiresAt);
    accessTokenRepository.create(ctx, token);

    accessTokenRepository.deleteByUserId(ctx, testUser.getUserId());

    assertThat(accessTokenRepository.findByUserIdAndToken(ctx, testUser.getUserId(), "token1"))
        .isEmpty();
  }

  @Test
  void deleteByUserIdShouldNotThrowWhenNoTokensExist() {
    assertThatCode(() -> accessTokenRepository.deleteByUserId(ctx, UUID.randomUUID()))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldRoundTripTokenWithLongExpirationTime() {
    Instant farFuture =
        Instant.now().plus(365 * 10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
    IssuedToken token = new IssuedToken("long-lived-token", testUser.getUserId(), farFuture);

    accessTokenRepository.create(ctx, token);

    Optional<IssuedToken> found =
        accessTokenRepository.findByUserIdAndToken(ctx, testUser.getUserId(), "long-lived-token");
    assertThat(found).isPresent();
    assertThat(found.get().expiresAt()).isEqualTo(farFuture);
  }

  @Test
  void shouldRoundTripTokenWithSpecialCharacters() {
    Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);
    String specialToken = "token+with/special=chars&symbols";
    IssuedToken token = new IssuedToken(specialToken, testUser.getUserId(), expiresAt);

    accessTokenRepository.create(ctx, token);

    Optional<IssuedToken> found =
        accessTokenRepository.findByUserIdAndToken(ctx, testUser.getUserId(), specialToken);
    assertThat(found).isPresent();
    assertThat(found.get().token()).isEqualTo(specialToken);
  }

  @Test
  void deleteByUserIdShouldNotAffectOtherUsersTokens() {
    AuthUser otherUser = AuthUser.create("otheruser");
    authUserRepository.create(ctx, otherUser);

    Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);
    IssuedToken testUserToken = new IssuedToken("test-user-token", testUser.getUserId(), expiresAt);
    IssuedToken otherUserToken =
        new IssuedToken("other-user-token", otherUser.getUserId(), expiresAt);
    accessTokenRepository.create(ctx, testUserToken);
    accessTokenRepository.create(ctx, otherUserToken);

    accessTokenRepository.deleteByUserId(ctx, testUser.getUserId());

    assertThat(
            accessTokenRepository.findByUserIdAndToken(
                ctx, testUser.getUserId(), "test-user-token"))
        .isEmpty();
    assertThat(
            accessTokenRepository.findByUserIdAndToken(
                ctx, otherUser.getUserId(), "other-user-token"))
        .isPresent();
  }
}
