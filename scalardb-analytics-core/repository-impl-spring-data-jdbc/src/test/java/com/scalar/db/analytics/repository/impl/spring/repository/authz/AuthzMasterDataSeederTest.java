/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.authz;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.domain.authz.ResourceType;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.authz.PermissionEntity;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.authz.PermissionJdbcRepository;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.authz.ResourceTypeEntity;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.authz.ResourceTypeJdbcRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthzMasterDataSeederTest {

  @Mock private ResourceTypeJdbcRepository resourceTypeJdbcRepository;
  @Mock private PermissionJdbcRepository permissionJdbcRepository;

  private AuthzMasterDataSeeder seeder;

  @BeforeEach
  void setUp() {
    seeder = new AuthzMasterDataSeeder(resourceTypeJdbcRepository, permissionJdbcRepository);
  }

  @Test
  void seedResourceTypes_shouldInsertAllWhenNoneExist() {
    when(resourceTypeJdbcRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

    seeder.seedResourceTypes();

    verify(resourceTypeJdbcRepository, times(ResourceType.values().length))
        .insert(any(ResourceTypeEntity.class));
  }

  @Test
  void seedResourceTypes_shouldSkipWhenAlreadyExist() {
    when(resourceTypeJdbcRepository.findById(any(UUID.class)))
        .thenReturn(Optional.of(mock(ResourceTypeEntity.class)));

    seeder.seedResourceTypes();

    verify(resourceTypeJdbcRepository, never()).insert(any(ResourceTypeEntity.class));
  }

  @Test
  void seedPermissions_shouldInsertAllWhenNoneExist() {
    when(permissionJdbcRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

    seeder.seedPermissions();

    verify(permissionJdbcRepository, times(Permission.values().length))
        .insert(any(PermissionEntity.class));
  }

  @Test
  void seedPermissions_shouldSkipWhenAlreadyExist() {
    when(permissionJdbcRepository.findById(any(UUID.class)))
        .thenReturn(Optional.of(mock(PermissionEntity.class)));

    seeder.seedPermissions();

    verify(permissionJdbcRepository, never()).insert(any(PermissionEntity.class));
  }
}
