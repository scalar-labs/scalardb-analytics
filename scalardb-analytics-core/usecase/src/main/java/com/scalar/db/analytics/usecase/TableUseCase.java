/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase;

import com.scalar.db.analytics.api.model.DataSourceNamespaceTable;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTableDetail;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TableUseCase {
  List<DataSourceNamespaceTable> listTables(UUID userId, String catalogName);

  List<DataSourceNamespaceTable> listTablesByNamespace(
      UUID userId, String catalogName, String dataSourceName, List<String> namespaceNames);

  Optional<DataSourceNamespaceTableDetail> describeTable(
      UUID userId,
      String catalogName,
      String dataSourceName,
      List<String> namespaceNames,
      String tableName);

  Optional<DataSourceNamespaceTableDetail> describeTableById(UUID userId, UUID tableId);
}
