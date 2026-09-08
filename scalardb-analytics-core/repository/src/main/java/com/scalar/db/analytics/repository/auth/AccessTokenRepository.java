/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.auth;

import com.scalar.db.analytics.domain.auth.IssuedToken;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing access tokens.
 *
 * <p>Each user has at most one active access token (1:1 relationship). Re-authentication replaces
 * the existing token. This design bounds token count to the number of users and eliminates the need
 * for expired token purging. See ADR-0005 for rationale.
 */
public interface AccessTokenRepository<T extends RepositoryTransactionContext> {
  Optional<IssuedToken> findByUserIdAndToken(T ctx, UUID userId, String token);

  void create(T ctx, IssuedToken token);

  void deleteByUserId(T ctx, UUID userId);
}
