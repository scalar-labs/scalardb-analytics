/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.domain.authz;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class BuiltInRoleTest {

  @Test
  void superadminIdShouldBeDeterministic() {
    UUID expected = UUID.nameUUIDFromBytes("built_in_role:SUPERADMIN".getBytes(UTF_8));
    assertThat(BuiltInRole.SUPERADMIN_ID).isEqualTo(expected);
  }

  @Test
  void superadminShouldReturnCorrectRole() {
    Role role = BuiltInRole.superadmin();

    assertThat(role.id()).isEqualTo(BuiltInRole.SUPERADMIN_ID);
    assertThat(role.name()).isEqualTo("SUPERADMIN");
    assertThat(role.builtIn()).isTrue();
  }

  @Test
  void superadminIdShouldNotCollideWithPermissionOrResourceTypeIds() {
    for (Permission permission : Permission.values()) {
      assertThat(BuiltInRole.SUPERADMIN_ID).isNotEqualTo(permission.id());
    }
    for (ResourceType resourceType : ResourceType.values()) {
      assertThat(BuiltInRole.SUPERADMIN_ID).isNotEqualTo(resourceType.id());
    }
  }
}
