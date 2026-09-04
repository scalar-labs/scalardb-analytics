/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.table;

import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTable;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTableDetail;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Client for table operations in ScalarDB Analytics. */
public interface TableClient {

  /**
   * Lists all tables in a catalog.
   *
   * @param catalogName the name of the catalog
   * @return a list of tables
   * @throws AnalyticsException if the operation fails
   */
  List<DataSourceNamespaceTable> listTablesByCatalog(String catalogName);

  /**
   * Lists tables in a specific namespace.
   *
   * @param catalogName the name of the catalog
   * @param dataSourceName the name of the data source
   * @param namespaceNames the namespace path
   * @return a list of tables
   * @throws AnalyticsException if the operation fails
   */
  List<DataSourceNamespaceTable> listTablesByNamespace(
      String catalogName, String dataSourceName, List<String> namespaceNames);

  /**
   * Describes a table, which returns detailed information about the table, including its columns.
   *
   * @param catalogName the name of the catalog
   * @param dataSourceName the name of the data source
   * @param namespaceNames the namespace path
   * @param tableName the name of the table
   * @return an Optional containing the table details if found, or empty if not found
   * @throws AnalyticsException if the operation fails
   */
  Optional<DataSourceNamespaceTableDetail> describeTableByName(
      String catalogName, String dataSourceName, List<String> namespaceNames, String tableName);

  /**
   * Describes a table by its ID, which returns detailed information about the table, including its
   * columns.
   *
   * @param tableId the ID of the table
   * @return an Optional containing the table details if found, or empty if not found
   * @throws AnalyticsException if the operation fails
   */
  Optional<DataSourceNamespaceTableDetail> describeTableById(UUID tableId);
}
