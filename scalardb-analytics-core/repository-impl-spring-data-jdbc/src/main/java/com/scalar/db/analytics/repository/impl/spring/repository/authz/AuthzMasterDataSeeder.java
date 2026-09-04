/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.authz;

import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.domain.authz.ResourceType;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.authz.PermissionEntity;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.authz.PermissionJdbcRepository;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.authz.ResourceTypeEntity;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.authz.ResourceTypeJdbcRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Seeds ResourceType and Permission master data into the database. Each operation is idempotent:
 * existing records are skipped.
 */
@Component
public class AuthzMasterDataSeeder {

  private static final Logger logger = LoggerFactory.getLogger(AuthzMasterDataSeeder.class);

  private final ResourceTypeJdbcRepository resourceTypeJdbcRepository;
  private final PermissionJdbcRepository permissionJdbcRepository;

  public AuthzMasterDataSeeder(
      ResourceTypeJdbcRepository resourceTypeJdbcRepository,
      PermissionJdbcRepository permissionJdbcRepository) {
    this.resourceTypeJdbcRepository = resourceTypeJdbcRepository;
    this.permissionJdbcRepository = permissionJdbcRepository;
  }

  /** Seeds all {@link ResourceType} enum values into the database if not already present. */
  public void seedResourceTypes() {
    for (ResourceType rt : ResourceType.values()) {
      if (resourceTypeJdbcRepository.findById(rt.id()).isEmpty()) {
        resourceTypeJdbcRepository.insert(new ResourceTypeEntity(rt.id(), rt.name()));
        logger.info("Seeded ResourceType: {}", rt.name());
      }
    }
  }

  /** Seeds all {@link Permission} enum values into the database if not already present. */
  public void seedPermissions() {
    for (Permission p : Permission.values()) {
      if (permissionJdbcRepository.findById(p.id()).isEmpty()) {
        permissionJdbcRepository.insert(
            new PermissionEntity(p.id(), p.name(), p.resourceType().id()));
        logger.info("Seeded Permission: {}", p.name());
      }
    }
  }
}
