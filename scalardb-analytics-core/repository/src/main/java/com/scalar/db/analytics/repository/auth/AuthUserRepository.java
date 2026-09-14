/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.auth;

import com.scalar.db.analytics.api.auth.AuthUser;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository for managing authentication users. */
public interface AuthUserRepository<T extends RepositoryTransactionContext> {
  Optional<AuthUser> findById(T ctx, UUID userId);

  /** Returns all principals (authentication users), regardless of any backend identities. */
  List<AuthUser> findAll(T ctx);

  // TODO: This method assumes username is unique, which holds under the current 1:1
  //  user-backend relationship. Revisit when supporting multiple backends per user, as
  //  different backends could produce users with the same username.
  Optional<AuthUser> findByUsername(T ctx, String username);

  void create(T ctx, AuthUser user);

  void deleteById(T ctx, UUID userId);
}
