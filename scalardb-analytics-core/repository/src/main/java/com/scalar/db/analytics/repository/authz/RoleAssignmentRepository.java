/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.authz;

import com.scalar.db.analytics.domain.authz.RoleAssignment;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import java.util.List;
import java.util.UUID;

/** Repository for managing role assignments to users. */
public interface RoleAssignmentRepository<T extends RepositoryTransactionContext> {
  List<RoleAssignment> findByUserId(T ctx, UUID userId);

  void create(T ctx, RoleAssignment assignment);

  void delete(T ctx, UUID userId, UUID roleId);

  void deleteByUserId(T ctx, UUID userId);

  void deleteByRoleId(T ctx, UUID roleId);

  List<RoleAssignment> findByRoleId(T ctx, UUID roleId);
}
