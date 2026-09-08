/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository;

import com.scalar.db.analytics.api.model.Catalog;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogRepository<T extends RepositoryTransactionContext> {
  Optional<Catalog> findById(T ctx, UUID id);

  Optional<Catalog> findByName(T ctx, String name);

  List<Catalog> list(T ctx);

  void create(T ctx, Catalog catalog);

  void deleteByName(T ctx, String name);

  void deleteById(T ctx, UUID id);
}
