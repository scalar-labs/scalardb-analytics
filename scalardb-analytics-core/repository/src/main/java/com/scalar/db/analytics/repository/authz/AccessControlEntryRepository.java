/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.authz;

import com.scalar.db.analytics.domain.authz.AccessControlEntry;
import com.scalar.db.analytics.domain.authz.GranteeType;
import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import java.util.List;
import java.util.UUID;

/** Repository for managing access control entries. */
public interface AccessControlEntryRepository<T extends RepositoryTransactionContext> {

  List<AccessControlEntry> findByGrantee(T ctx, GranteeType granteeType, UUID granteeId);

  void create(T ctx, AccessControlEntry entry);

  void delete(
      T ctx, GranteeType granteeType, UUID granteeId, UUID resourceId, Permission permission);

  void deleteByGrantee(T ctx, GranteeType granteeType, UUID granteeId);

  void deleteByResourceId(T ctx, UUID resourceId);
}
