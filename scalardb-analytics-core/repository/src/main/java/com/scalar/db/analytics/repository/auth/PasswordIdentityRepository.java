/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.auth;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.domain.auth.PasswordIdentity;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing password identity mappings. Maps authentication backend identity (backend
 * + backend_user_id) to internal user.
 *
 * <p>The natural key of a {@link PasswordIdentity} is the pair {@code (backend, backendUserId)};
 * the same {@code backendUserId} string can legitimately exist under different backends. APIs that
 * operate on individual identities therefore take both halves of the natural key. Methods that
 * delete by {@code userId} alone are reserved for principal-cascade deletion, where removing every
 * identity for the user across backends is the correct semantic.
 */
public interface PasswordIdentityRepository<T extends RepositoryTransactionContext> {
  /**
   * Returns the identity matching the natural key {@code (backend, backendUserId)}, or empty if no
   * such identity exists.
   */
  Optional<PasswordIdentity> findByBackendAndBackendUserId(
      T ctx, PasswordBackendType backend, String backendUserId);

  List<PasswordIdentity> findByUserId(T ctx, UUID userId);

  void create(T ctx, PasswordIdentity identity);

  List<PasswordIdentity> findByBackend(T ctx, PasswordBackendType backend);

  /**
   * Removes every identity held by the given principal across every backend. Intended for
   * principal-cascade deletion; for unlinking a single backend identity from a principal, use
   * {@link #deleteByUserIdAndBackend(RepositoryTransactionContext, UUID, PasswordBackendType)}.
   */
  void deleteByUserId(T ctx, UUID userId);

  /**
   * Removes the identity (if any) held by the given principal on the given backend, leaving the
   * principal's identities on other backends intact. This is the inverse of {@code
   * UserUseCase.linkBackendUser}'s one-identity-per-(principal, backend) invariant.
   */
  void deleteByUserIdAndBackend(T ctx, UUID userId, PasswordBackendType backend);
}
