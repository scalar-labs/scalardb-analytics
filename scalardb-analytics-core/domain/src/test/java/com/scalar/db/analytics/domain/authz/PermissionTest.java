/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.domain.authz;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PermissionTest {

  @ParameterizedTest
  @EnumSource(Permission.class)
  void allPermissionsHaveAssociatedResourceType(Permission permission) {
    assertThat(permission.resourceType()).isNotNull();
  }

  @ParameterizedTest
  @EnumSource(Permission.class)
  void allPermissionsHaveDeterministicId(Permission permission) {
    UUID id1 = permission.id();
    UUID id2 = permission.id();
    assertThat(id1).isNotNull().isEqualTo(id2);
  }

  @Test
  void allPermissionsHaveUniqueIds() {
    Set<UUID> ids = new HashSet<>();
    for (Permission permission : Permission.values()) {
      assertThat(ids.add(permission.id())).as("Duplicate id for " + permission.name()).isTrue();
    }
  }

  @Test
  void catalogPermissionsHaveCatalogResourceType() {
    assertThat(Permission.CATALOG_READ.resourceType()).isEqualTo(ResourceType.CATALOG);
    assertThat(Permission.CATALOG_WRITE.resourceType()).isEqualTo(ResourceType.CATALOG);
    assertThat(Permission.CATALOG_ADMIN.resourceType()).isEqualTo(ResourceType.CATALOG);
  }

  @Test
  void dataSourcePermissionsHaveDataSourceResourceType() {
    assertThat(Permission.DATA_SOURCE_READ.resourceType()).isEqualTo(ResourceType.DATA_SOURCE);
    assertThat(Permission.DATA_SOURCE_ADMIN.resourceType()).isEqualTo(ResourceType.DATA_SOURCE);
  }

  @Test
  void namespacePermissionHasNamespaceResourceType() {
    assertThat(Permission.NAMESPACE_READ.resourceType()).isEqualTo(ResourceType.NAMESPACE);
  }

  @Test
  void tablePermissionHasTableResourceType() {
    assertThat(Permission.TABLE_READ.resourceType()).isEqualTo(ResourceType.TABLE);
  }
}
