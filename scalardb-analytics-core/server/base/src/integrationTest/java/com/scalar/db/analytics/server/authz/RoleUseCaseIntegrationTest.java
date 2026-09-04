/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.authz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.analytics.api.auth.UserDetail;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.authz.Role;
import com.scalar.db.analytics.server.support.UseCaseIntegrationTestBase;
import com.scalar.db.analytics.usecase.auth.UserUseCase;
import com.scalar.db.analytics.usecase.authz.RoleUseCase;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RoleUseCaseIntegrationTest extends UseCaseIntegrationTestBase {

  @Autowired private RoleUseCase roleUseCase;
  @Autowired private UserUseCase userUseCase;

  @AfterEach
  void cleanUp() {
    UUID adminId = adminUserId();
    for (String roleName : new String[] {"test-role", "temp-role"}) {
      try {
        roleUseCase.deleteRole(adminId, roleName);
      } catch (AnalyticsException ignored) {
      }
    }
    try {
      userUseCase.deleteUser(adminId, "temp-user", true);
    } catch (AnalyticsException ignored) {
    }
  }

  @Test
  void listRoles_shouldContainSuperadmin() {
    UUID adminId = adminUserId();
    var roles = roleUseCase.listRoles(adminId);

    assertThat(roles).extracting(Role::name).contains("SUPERADMIN");
    assertThat(roles).anyMatch(r -> r.name().equals("SUPERADMIN") && r.builtIn());
  }

  @Test
  void createAndDeleteRole_shouldWork() {
    UUID adminId = adminUserId();

    Role created = roleUseCase.createRole(adminId, "test-role");
    assertThat(created.name()).isEqualTo("test-role");
    assertThat(created.builtIn()).isFalse();

    var roles = roleUseCase.listRoles(adminId);
    assertThat(roles).extracting(Role::name).contains("test-role");

    boolean deleted = roleUseCase.deleteRole(adminId, "test-role");
    assertThat(deleted).isTrue();

    roles = roleUseCase.listRoles(adminId);
    assertThat(roles).extracting(Role::name).doesNotContain("test-role");
  }

  @Test
  void deleteBuiltInRole_shouldFail() {
    UUID adminId = adminUserId();

    assertThatThrownBy(() -> roleUseCase.deleteRole(adminId, "SUPERADMIN"))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            ex ->
                assertThat(((AnalyticsException) ex).getErrorCode())
                    .isEqualTo(AnalyticsErrorCode.BUILT_IN_ENTITY_NOT_MODIFIABLE))
        .hasMessageContaining("SUPERADMIN");
  }

  @Test
  void grantAndRevokeRole_shouldWork() {
    UUID adminId = adminUserId();

    roleUseCase.createRole(adminId, "temp-role");
    userUseCase.createUserWithBackendUser(adminId, "temp-user", "temp-user", "temp-pass");

    roleUseCase.grantRole(adminId, "temp-role", "temp-user");
    UserDetail afterGrant = userUseCase.describeUserByName(adminId, "temp-user").orElseThrow();
    assertThat(afterGrant.getRoles()).contains("temp-role");

    roleUseCase.revokeRole(adminId, "temp-role", "temp-user");
    UserDetail afterRevoke = userUseCase.describeUserByName(adminId, "temp-user").orElseThrow();
    assertThat(afterRevoke.getRoles()).doesNotContain("temp-role");
  }
}
