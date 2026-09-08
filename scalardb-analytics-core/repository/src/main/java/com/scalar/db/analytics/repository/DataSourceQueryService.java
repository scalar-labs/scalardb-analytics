/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository;

import com.scalar.db.analytics.api.model.DataSource;
import java.util.List;
import java.util.Optional;

/** Query service for complex DataSource queries involving joins with other tables. */
public interface DataSourceQueryService<T extends RepositoryTransactionContext> {

  /**
   * Finds a DataSource by catalog name and data source name. This requires joining data_sources
   * with catalogs table.
   *
   * @param ctx the transaction context
   * @param catalogName the catalog name
   * @param dataSourceName the data source name
   * @return the data source if found
   */
  Optional<DataSource> findByName(T ctx, String catalogName, String dataSourceName);

  /**
   * Lists all data sources in a catalog by catalog name. This requires joining data_sources with
   * catalogs table.
   *
   * @param ctx the transaction context
   * @param catalogName the catalog name
   * @return list of data sources
   */
  List<DataSource> listByCatalogName(T ctx, String catalogName);
}
