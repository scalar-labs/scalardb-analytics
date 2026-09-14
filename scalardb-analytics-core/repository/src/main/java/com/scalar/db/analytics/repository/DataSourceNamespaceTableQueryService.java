/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository;

import com.scalar.db.analytics.api.model.DataSourceNamespaceTable;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTableDetail;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DataSourceNamespaceTableQueryService<T extends RepositoryTransactionContext> {
  List<DataSourceNamespaceTable> listByCatalogName(T ctx, String catalogName);

  List<DataSourceNamespaceTable> listByNamespaceId(T ctx, UUID namespaceId);

  List<DataSourceNamespaceTable> listByNamespaceNames(
      T ctx, String catalogName, String dataSourceName, List<String> namespaceNames);

  Optional<DataSourceNamespaceTableDetail> findDetailByName(
      T ctx,
      String catalogName,
      String dataSourceName,
      List<String> namespaceNames,
      String tableName);

  Optional<DataSourceNamespaceTableDetail> findDetailById(T ctx, UUID tableId);
}
