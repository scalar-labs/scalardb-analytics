/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.auth.IssuedToken;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.auth.AccessTokenRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link TokenValidationUseCase}.
 *
 * <p>Validates access tokens by looking them up in the database and checking expiration. Validated
 * tokens are cached in-memory to reduce database load.
 *
 * @param <T> the type of transaction context
 */
public class TokenValidationUseCaseImpl<T extends RepositoryTransactionContext>
    implements TokenValidationUseCase {

  private static final Logger logger = LoggerFactory.getLogger(TokenValidationUseCaseImpl.class);
  static final Duration DEFAULT_CACHE_TTL = Duration.ofSeconds(60);
  static final long DEFAULT_CACHE_MAX_SIZE = 1000;

  private final AccessTokenRepository<T> accessTokenRepository;
  private final RepositoryTransactionManager<T> txManager;
  private final Clock clock;
  private final Cache<String, UUID> tokenCache;

  public TokenValidationUseCaseImpl(
      AccessTokenRepository<T> accessTokenRepository,
      RepositoryTransactionManager<T> txManager,
      Clock clock) {
    this(accessTokenRepository, txManager, clock, DEFAULT_CACHE_TTL, DEFAULT_CACHE_MAX_SIZE);
  }

  public TokenValidationUseCaseImpl(
      AccessTokenRepository<T> accessTokenRepository,
      RepositoryTransactionManager<T> txManager,
      Clock clock,
      Duration cacheTtl,
      long cacheMaxSize) {
    this.accessTokenRepository = accessTokenRepository;
    this.txManager = txManager;
    this.clock = clock;
    this.tokenCache =
        Caffeine.newBuilder().expireAfterWrite(cacheTtl).maximumSize(cacheMaxSize).build();
  }

  @Override
  public UUID validateToken(UUID userId, String token) {
    // Cache hit skips expiration check intentionally. The cache uses expireAfterWrite, so entries
    // are evicted within the TTL window regardless of access. The maximum staleness (equal to the
    // cache TTL) is acceptable for this use case.
    String cacheKey = userId + ":" + token;
    UUID cachedUserId = tokenCache.getIfPresent(cacheKey);
    if (cachedUserId != null) {
      return cachedUserId;
    }

    UUID validatedUserId = validateTokenFromDb(userId, token);
    tokenCache.put(cacheKey, validatedUserId);
    return validatedUserId;
  }

  private UUID validateTokenFromDb(UUID userId, String token) {
    return txManager.withTransaction(
        ctx -> {
          IssuedToken issuedToken =
              accessTokenRepository
                  .findByUserIdAndToken(ctx, userId, token)
                  .orElseThrow(
                      () -> {
                        logger.warn("Token validation failed: token not found");
                        return new AnalyticsException(AnalyticsErrorCode.TOKEN_INVALID);
                      });

          Instant now = clock.instant();
          if (issuedToken.isExpiredAt(now)) {
            logger.warn("Token validation failed: token expired at {}", issuedToken.expiresAt());
            throw new AnalyticsException(AnalyticsErrorCode.TOKEN_EXPIRED);
          }

          return issuedToken.userId();
        });
  }
}
