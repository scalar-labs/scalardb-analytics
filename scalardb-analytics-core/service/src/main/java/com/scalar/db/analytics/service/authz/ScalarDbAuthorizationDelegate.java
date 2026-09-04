/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.authz;

import java.util.List;
import java.util.UUID;

/**
 * Delegate for authorizing ScalarDB Cluster privileges.
 *
 * <p>For ScalarDB data source resources (Namespace, Table), this delegate is the sole authorization
 * authority — Analytics ACL is not evaluated for these resources. The two authorization paths are
 * mutually exclusive per resource.
 *
 * <p>The delegate silently skips (returns {@code true}) when:
 *
 * <ul>
 *   <li>None of the requirements target ScalarDB data source resources
 *   <li>The user does not have a ScalarDB Cluster backend identity
 * </ul>
 *
 * @see AuthorizationServiceImpl
 */
public interface ScalarDbAuthorizationDelegate {

  /**
   * Authorizes the given user against ScalarDB Cluster privileges.
   *
   * @param userId the internal user ID
   * @param requirements the permission requirements to check
   * @return {@code true} if the user has the required ScalarDB privileges or the check is skipped
   */
  boolean authorize(UUID userId, List<PermissionRequirement> requirements);
}
