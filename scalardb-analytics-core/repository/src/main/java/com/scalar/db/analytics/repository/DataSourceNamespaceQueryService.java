/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository;

import com.scalar.db.analytics.api.model.DataSourceNamespace;
import java.util.List;
import java.util.Optional;

public interface DataSourceNamespaceQueryService<T extends RepositoryTransactionContext> {
  List<DataSourceNamespace> listByCatalogName(T ctx, String catalogName);

  List<DataSourceNamespace> listByDataSourceId(T ctx, java.util.UUID dataSourceId);

  Optional<DataSourceNamespace> findByCatalogAndDataSourceAndNames(
      T ctx, String catalogName, String dataSourceName, List<String> namespaceNames);
}
