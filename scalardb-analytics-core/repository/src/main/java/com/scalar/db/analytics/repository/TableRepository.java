/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository;

import com.scalar.db.analytics.api.model.Table;
import com.scalar.db.analytics.api.model.TableDetail;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TableRepository<T extends RepositoryTransactionContext> {
  Optional<Table> findById(T ctx, UUID id);

  List<Table> listByNamespaceId(T ctx, UUID namespaceId);

  Optional<Table> findByNamespaceIdAndName(T ctx, UUID namespaceId, String name);

  void create(T ctx, TableDetail table);

  void deleteById(T ctx, UUID id);
}
