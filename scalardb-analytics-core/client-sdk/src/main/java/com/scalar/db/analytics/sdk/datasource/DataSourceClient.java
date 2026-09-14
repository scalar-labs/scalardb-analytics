/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.datasource;

import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Client for data source operations in ScalarDB Analytics. */
public interface DataSourceClient {

  /**
   * Finds a data source by name.
   *
   * @param catalogName the name of the catalog
   * @param dataSourceName the name of the data source to find
   * @return an Optional containing the data source if found, or empty if not found
   * @throws AnalyticsException if the operation fails
   */
  Optional<DataSource> findDataSourceByName(String catalogName, String dataSourceName);

  /**
   * Describes a data source by its ID.
   *
   * @param dataSourceId the ID of the data source
   * @return an Optional containing the data source if found, or empty if not found
   * @throws AnalyticsException if the operation fails
   */
  Optional<DataSource> findDataSourceById(UUID dataSourceId);

  /**
   * Lists all data sources in a catalog.
   *
   * @param catalogName the name of the catalog
   * @return a list of data sources
   * @throws AnalyticsException if the operation fails
   */
  List<DataSource> listDataSourcesByCatalog(String catalogName);

  /**
   * Registers a new data source.
   *
   * @param request the data source registration request
   * @return the registered data source
   * @throws AnalyticsException if the operation fails
   */
  DataSource register(RegisterDataSourceRequest request);

  /**
   * Deletes a data source by name.
   *
   * @param catalogName the name of the catalog
   * @param dataSourceName the name of the data source to delete
   * @param cascade if true, cascade delete all associated namespaces, tables, and columns
   * @return true if the data source was deleted, false if it didn't exist
   * @throws AnalyticsException if the operation fails or if cascade is false and children exist
   */
  boolean deleteDataSourceByName(String catalogName, String dataSourceName, boolean cascade);

  /**
   * Deletes a data source by ID.
   *
   * @param dataSourceId the ID of the data source to delete
   * @param cascade if true, cascade delete all associated namespaces, tables, and columns
   * @return true if the data source was deleted, false if it didn't exist
   * @throws AnalyticsException if the operation fails or if cascade is false and children exist
   */
  boolean deleteDataSourceById(UUID dataSourceId, boolean cascade);
}
