/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository;

import com.scalar.db.analytics.api.model.DataSource;
import java.util.Optional;
import java.util.UUID;

public interface DataSourceRepository<T extends RepositoryTransactionContext> {
  Optional<DataSource> findById(T ctx, UUID id);

  // Required for staged resolution in ScalarDB SQL implementation
  Optional<DataSource> findByCatalogIdAndName(T ctx, UUID catalogId, String name);

  void create(T ctx, DataSource dataSource);

  void deleteById(T ctx, UUID id);
}
