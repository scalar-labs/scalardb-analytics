/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.authz;

import org.jspecify.annotations.Nullable;

/**
 * Client for checking ScalarDB Cluster privileges via the HasPrivilege API.
 *
 * <p>Implementations handle caching. The caller is responsible for providing a valid ScalarDB
 * Cluster auth token.
 */
public interface ScalarDbPrivilegeClient {

  /**
   * Checks if the specified user has SELECT privilege on a namespace or table.
   *
   * @param authToken the ScalarDB Cluster auth token for the calling user
   * @param username the ScalarDB username to check
   * @param namespaceName the namespace name
   * @param tableName the table name, or {@code null} for namespace-level check
   * @return {@code true} if the user has the privilege
   */
  boolean hasSelectPrivilege(
      String authToken, String username, String namespaceName, @Nullable String tableName);
}
