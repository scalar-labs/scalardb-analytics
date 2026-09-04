/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.namespace;

import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.DataSourceNamespace;
import com.scalar.db.analytics.api.model.Namespace;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Client for namespace operations in ScalarDB Analytics. */
public interface NamespaceClient {

  /**
   * Lists all namespaces in a catalog.
   *
   * @param catalogName the name of the catalog
   * @return a list of namespaces
   * @throws AnalyticsException if the operation fails
   */
  List<DataSourceNamespace> listNamespacesByCatalog(String catalogName);

  /**
   * Describes a namespace.
   *
   * @param catalogName the name of the catalog
   * @param dataSourceName the name of the data source
   * @param namespaceNames the namespace path
   * @return an Optional containing the namespace if found, or empty if not found
   * @throws AnalyticsException if the operation fails
   */
  Optional<Namespace> findNamespaceByName(
      String catalogName, String dataSourceName, List<String> namespaceNames);

  /**
   * Describes a namespace by its ID.
   *
   * @param namespaceId the ID of the namespace
   * @return an Optional containing the namespace if found, or empty if not found
   * @throws AnalyticsException if the operation fails
   */
  Optional<Namespace> findNamespaceById(UUID namespaceId);
}
