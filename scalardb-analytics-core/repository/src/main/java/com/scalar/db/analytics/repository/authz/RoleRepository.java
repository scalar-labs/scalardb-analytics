/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.authz;

import com.scalar.db.analytics.domain.authz.Role;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository for managing roles. */
public interface RoleRepository<T extends RepositoryTransactionContext> {
  Optional<Role> findById(T ctx, UUID roleId);

  Optional<Role> findByName(T ctx, String name);

  List<Role> findAll(T ctx);

  void create(T ctx, Role role);

  void deleteById(T ctx, UUID roleId);
}
