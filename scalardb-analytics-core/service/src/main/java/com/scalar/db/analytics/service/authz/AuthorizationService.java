/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.authz;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/** Domain service for authorization checks on resource operations. */
public interface AuthorizationService {

  /**
   * Checks if the user has the SUPERADMIN role. Used for operations restricted to SUPERADMIN only
   * (e.g., CreateCatalog, RegisterUser).
   *
   * @param userId the user ID to check
   * @return true if authorized, false otherwise
   */
  boolean authorizeSuperAdmin(UUID userId);

  /**
   * Checks if the user is authorized based on permission requirements.
   *
   * <p>Requirements are OR-ed across hierarchy levels: the user must satisfy at least one.
   * SUPERADMIN users bypass all checks.
   *
   * @param userId the user ID to check
   * @param requirements the permission requirements at each resource hierarchy level
   * @return true if authorized, false otherwise
   */
  boolean authorize(UUID userId, List<PermissionRequirement> requirements);

  /**
   * Checks if the user is authorized to manage permissions for the specified catalog.
   *
   * <p>Authorized if the user is a SUPERADMIN or holds a CATALOG_ADMIN ACE on the target catalog.
   *
   * @param userId the user ID to check
   * @param catalogId the catalog ID for which permission management is requested
   * @return true if authorized, false otherwise
   */
  boolean authorizePermissionManagement(UUID userId, UUID catalogId);

  /**
   * Filters items, returning only those the user is authorized to access.
   *
   * <p>Opens a single transaction and fetches the user's authorization context (role assignments
   * and ACEs) once. SUPERADMIN users get all items returned unfiltered. For other users, each
   * item's requirements are evaluated in-memory against the pre-fetched ACEs.
   *
   * @param <R> the type of items to filter
   * @param userId the user ID to check authorization for
   * @param items the items to filter
   * @param requirementsMapper a function that extracts permission requirements from each item
   * @return the filtered list of authorized items
   */
  <R> List<R> filterAuthorized(
      UUID userId, List<R> items, Function<R, List<PermissionRequirement>> requirementsMapper);
}
