/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.auth.IssuedToken;
import com.scalar.db.analytics.lib.functional.ThrowableFunction;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.auth.AccessTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenValidationUseCaseImplTest {

  private static final Instant NOW = Instant.parse("2025-01-01T00:00:00Z");

  @Mock private AccessTokenRepository<RepositoryTransactionContext> accessTokenRepository;
  @Mock private RepositoryTransactionManager<RepositoryTransactionContext> txManager;

  private TokenValidationUseCaseImpl<RepositoryTransactionContext> useCase;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    useCase = new TokenValidationUseCaseImpl<>(accessTokenRepository, txManager, clock);
  }

  @SuppressWarnings("unchecked")
  private void stubTransactionPassthrough() throws Exception {
    when(txManager.withTransaction(any()))
        .thenAnswer(
            invocation -> {
              var function =
                  (ThrowableFunction<RepositoryTransactionContext, UUID, Throwable>)
                      invocation.getArgument(0);
              RepositoryTransactionContext ctx = mock(RepositoryTransactionContext.class);
              return function.apply(ctx);
            });
  }

  @Test
  void validateToken_shouldReturnUserIdForValidToken() throws Exception {
    stubTransactionPassthrough();

    UUID userId = UUID.randomUUID();
    String token = "valid-token";
    Instant expiresAt = NOW.plusSeconds(3600);
    IssuedToken issuedToken = new IssuedToken(token, userId, expiresAt);

    when(accessTokenRepository.findByUserIdAndToken(any(), eq(userId), eq(token)))
        .thenReturn(Optional.of(issuedToken));

    UUID result = useCase.validateToken(userId, token);

    assertThat(result).isEqualTo(userId);
  }

  @Test
  void validateToken_shouldThrowOnUnknownToken() throws Exception {
    stubTransactionPassthrough();

    UUID userId = UUID.randomUUID();
    String token = "unknown-token";

    when(accessTokenRepository.findByUserIdAndToken(any(), eq(userId), eq(token)))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.validateToken(userId, token))
        .isInstanceOf(AnalyticsException.class)
        .extracting(ex -> ((AnalyticsException) ex).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.TOKEN_INVALID);
  }

  @Test
  void validateToken_shouldThrowOnExpiredToken() throws Exception {
    stubTransactionPassthrough();

    UUID userId = UUID.randomUUID();
    String token = "expired-token";
    Instant expiresAt = NOW.minusSeconds(1);
    IssuedToken issuedToken = new IssuedToken(token, userId, expiresAt);

    when(accessTokenRepository.findByUserIdAndToken(any(), eq(userId), eq(token)))
        .thenReturn(Optional.of(issuedToken));

    assertThatThrownBy(() -> useCase.validateToken(userId, token))
        .isInstanceOf(AnalyticsException.class)
        .extracting(ex -> ((AnalyticsException) ex).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.TOKEN_EXPIRED);
  }

  @SuppressWarnings("unchecked")
  @Test
  void validateToken_shouldThrowAnalyticsExceptionOnDbFailure() throws Exception {
    doThrow(new AnalyticsException(AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED))
        .when(txManager)
        .withTransaction(any());

    UUID userId = UUID.randomUUID();
    assertThatThrownBy(() -> useCase.validateToken(userId, "any-token"))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            ex ->
                assertThat(((AnalyticsException) ex).getErrorCode())
                    .isEqualTo(AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED));
  }

  @Test
  void validateToken_shouldReturnCachedResultOnSecondCall() throws Exception {
    stubTransactionPassthrough();

    UUID userId = UUID.randomUUID();
    String token = "cached-token";
    Instant expiresAt = NOW.plusSeconds(3600);
    IssuedToken issuedToken = new IssuedToken(token, userId, expiresAt);

    when(accessTokenRepository.findByUserIdAndToken(any(), eq(userId), eq(token)))
        .thenReturn(Optional.of(issuedToken));

    UUID firstResult = useCase.validateToken(userId, token);
    UUID secondResult = useCase.validateToken(userId, token);

    assertThat(firstResult).isEqualTo(userId);
    assertThat(secondResult).isEqualTo(userId);
    // DB should only be called once; second call served from cache
    verify(txManager, times(1)).withTransaction(any());
  }

  @Test
  void validateToken_shouldNotCacheInvalidTokens() throws Exception {
    stubTransactionPassthrough();

    UUID userId = UUID.randomUUID();
    String token = "invalid-token";

    when(accessTokenRepository.findByUserIdAndToken(any(), eq(userId), eq(token)))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.validateToken(userId, token))
        .isInstanceOf(AnalyticsException.class);
    assertThatThrownBy(() -> useCase.validateToken(userId, token))
        .isInstanceOf(AnalyticsException.class);

    // DB should be called both times since invalid tokens are not cached
    verify(txManager, times(2)).withTransaction(any());
  }
}
