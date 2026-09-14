/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.authz;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * No-op implementation of {@link AuthorizationService} that allows all operations.
 *
 * <p>Used when authorization is disabled (i.e., {@code auth.enabled=false}).
 */
public class NoOpAuthorizationService implements AuthorizationService {

  @Override
  public boolean authorizeSuperAdmin(UUID userId) {
    // No-op: always allowed
    return true;
  }

  @Override
  public boolean authorize(UUID userId, List<PermissionRequirement> requirements) {
    // No-op: always allowed
    return true;
  }

  @Override
  public boolean authorizePermissionManagement(UUID userId, UUID catalogId) {
    // No-op: always allowed
    return true;
  }

  @Override
  public <R> List<R> filterAuthorized(
      UUID userId, List<R> items, Function<R, List<PermissionRequirement>> requirementsMapper) {
    // No-op: all items allowed
    return List.copyOf(items);
  }
}
