/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.domain.authz;

import java.util.UUID;

/**
 * Links a user to a role.
 *
 * @param userId the user being assigned
 * @param roleId the role being assigned to the user
 */
public record RoleAssignment(UUID userId, UUID roleId) {}
