/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase;

import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogUseCase {
  Catalog createCatalog(UUID userId, String catalogName);

  Optional<Catalog> findCatalog(UUID userId, String catalogName);

  Optional<Catalog> describeCatalogById(UUID userId, UUID catalogId);

  List<Catalog> listAllCatalogs(UUID userId);

  /**
   * Initializes a catalog with the given data sources. If the catalog already exists, it returns an
   * empty Optional.
   *
   * @param userId the authenticated user ID
   * @param catalogName the name of the catalog to be initialized
   * @param dataSources the list of data sources to be registered
   * @return the initialized catalog, or an empty Optional if the catalog already exists
   */
  Optional<Catalog> initializeCatalog(
      UUID userId, String catalogName, List<RegisterDataSourceRequest> dataSources);

  /**
   * Deletes a catalog by name.
   *
   * @param userId the authenticated user ID
   * @param catalogName the name of the catalog to delete
   * @param cascade if true, cascade delete all associated data sources and their children
   * @return true if the catalog was deleted, false if it didn't exist
   */
  boolean deleteCatalog(UUID userId, String catalogName, boolean cascade);

  /**
   * Deletes a catalog by ID.
   *
   * @param userId the authenticated user ID
   * @param catalogId the ID of the catalog to delete
   * @param cascade if true, cascade delete all associated data sources and their children
   * @return true if the catalog was deleted, false if it didn't exist
   */
  boolean deleteCatalogById(UUID userId, UUID catalogId, boolean cascade);
}
