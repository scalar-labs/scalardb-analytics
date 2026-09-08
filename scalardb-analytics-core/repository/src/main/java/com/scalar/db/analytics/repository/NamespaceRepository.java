/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository;

import com.scalar.db.analytics.api.model.Namespace;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NamespaceRepository<T extends RepositoryTransactionContext> {
  Optional<Namespace> findById(T ctx, UUID id);

  Optional<Namespace> findByDataSourceIdAndNames(T ctx, UUID dataSourceId, List<String> names);

  void create(T ctx, Namespace namespace);

  void deleteById(T ctx, UUID id);
}
