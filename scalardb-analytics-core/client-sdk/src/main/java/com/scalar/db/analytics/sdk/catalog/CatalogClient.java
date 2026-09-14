/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.catalog;

import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.Catalog;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Client for catalog operations in ScalarDB Analytics. */
public interface CatalogClient {

  /**
   * Creates a new catalog.
   *
   * @param catalogName the name of the catalog to create
   * @return the created catalog
   * @throws AnalyticsException if the operation fails
   */
  Catalog createCatalog(String catalogName);

  /**
   * Finds a catalog by name.
   *
   * @param catalogName the name of the catalog to find
   * @return an Optional containing the catalog if found, or empty if not found
   * @throws AnalyticsException if the operation fails
   */
  Optional<Catalog> findCatalogByName(String catalogName);

  /**
   * Describes a catalog by its ID.
   *
   * @param catalogId the ID of the catalog
   * @return an Optional containing the catalog if found, or empty if not found
   * @throws AnalyticsException if the operation fails
   */
  Optional<Catalog> findCatalogById(UUID catalogId);

  /**
   * Lists all catalogs.
   *
   * @return a list of all catalogs
   * @throws AnalyticsException if the operation fails
   */
  List<Catalog> listAllCatalogs();

  /**
   * Deletes a catalog by name.
   *
   * @param catalogName the name of the catalog to delete
   * @param cascade if true, cascade delete all associated data sources and their children
   * @return true if the catalog was deleted, false if it didn't exist
   * @throws AnalyticsException if the operation fails or if cascade is false and children exist
   */
  boolean deleteCatalogByName(String catalogName, boolean cascade);

  /**
   * Deletes a catalog by ID.
   *
   * @param catalogId the ID of the catalog to delete
   * @param cascade if true, cascade delete all associated data sources and their children
   * @return true if the catalog was deleted, false if it didn't exist
   * @throws AnalyticsException if the operation fails or if cascade is false and children exist
   */
  boolean deleteCatalogById(UUID catalogId, boolean cascade);
}
