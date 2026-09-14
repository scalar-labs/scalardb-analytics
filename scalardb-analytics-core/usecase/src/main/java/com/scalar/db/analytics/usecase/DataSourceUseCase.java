/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase;

import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DataSourceUseCase {
  Optional<DataSource> findDataSource(UUID userId, String catalogName, String dataSourceName);

  Optional<DataSource> describeDataSourceById(UUID userId, UUID dataSourceId);

  List<DataSource> listDataSources(UUID userId, String catalogName);

  DataSource register(UUID userId, RegisterDataSourceRequest request);

  /**
   * Deletes a data source by name.
   *
   * @param userId the authenticated user ID
   * @param catalogName the name of the catalog
   * @param dataSourceName the name of the data source to delete
   * @param cascade if true, cascade delete all associated namespaces, tables, and columns
   * @return true if the data source was deleted, false if it didn't exist
   */
  boolean deleteDataSource(UUID userId, String catalogName, String dataSourceName, boolean cascade);

  /**
   * Deletes a data source by ID.
   *
   * @param userId the authenticated user ID
   * @param dataSourceId the ID of the data source to delete
   * @param cascade if true, cascade delete all associated namespaces, tables, and columns
   * @return true if the data source was deleted, false if it didn't exist
   */
  boolean deleteDataSourceById(UUID userId, UUID dataSourceId, boolean cascade);
}
