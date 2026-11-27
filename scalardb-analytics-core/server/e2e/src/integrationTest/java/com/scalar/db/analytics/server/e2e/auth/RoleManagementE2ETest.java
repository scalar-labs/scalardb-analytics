/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.e2e.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.analytics.grpc.generated.auth.v1.AuthenticateWithPasswordResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.CreateRoleRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.CreateUserWithBackendUserRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteRoleRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteUserRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.DescribeUserByNameRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantRoleRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListRolesRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeRoleRequest;
import com.scalar.db.analytics.server.e2e.support.AuthE2ETestBase;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

/** E2E tests for role management. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoleManagementE2ETest extends AuthE2ETestBase {

  private static final String ALICE_USERNAME = "alice";
  private static final String ALICE_PASSWORD = "alice-password";

  @AfterAll
  void cleanUp() {
    // Remove resources created by this class so the shared DB stays clean for other E2E classes,
    // even if a test failed mid-scenario.
    AuthenticateWithPasswordResponse admin = authenticateAdmin();
    try {
      userStub(admin.getUserId(), admin.getToken())
          .deleteUser(
              DeleteUserRequest.newBuilder().setUsername(ALICE_USERNAME).setCascade(true).build());
    } catch (StatusRuntimeException ignored) {
      // May not exist
    }
    try {
      roleStub(admin.getUserId(), admin.getToken())
          .deleteRole(DeleteRoleRequest.newBuilder().setRoleName("analyst").build());
    } catch (StatusRuntimeException ignored) {
      // May not exist
    }
  }

  @Test
  @Order(1)
  void listRoles_shouldShowSuperadmin() {
    // Role list
    AuthenticateWithPasswordResponse admin = authenticateAdmin();
    var response =
        roleStub(admin.getUserId(), admin.getToken())
            .listRoles(ListRolesRequest.getDefaultInstance());

    assertThat(response.getRolesList())
        .anyMatch(r -> r.getName().equals("SUPERADMIN") && r.getBuiltIn());
  }

  @Test
  @Order(2)
  void createCustomRole_shouldSucceed() {
    // Custom role creation
    AuthenticateWithPasswordResponse admin = authenticateAdmin();
    var response =
        roleStub(admin.getUserId(), admin.getToken())
            .createRole(CreateRoleRequest.newBuilder().setRoleName("analyst").build());

    assertThat(response.getRole().getName()).isEqualTo("analyst");
    assertThat(response.getRole().getBuiltIn()).isFalse();
  }

  @Test
  @Order(3)
  void grantRole_shouldSucceed() {
    // Grant role to user
    AuthenticateWithPasswordResponse admin = authenticateAdmin();

    // Create alice (principal + internal-backend credential, atomic).
    userStub(admin.getUserId(), admin.getToken())
        .createUserWithBackendUser(
            CreateUserWithBackendUserRequest.newBuilder()
                .setUsername(ALICE_USERNAME)
                .setBackendUsername(ALICE_USERNAME)
                .setPassword(ALICE_PASSWORD)
                .build());

    roleStub(admin.getUserId(), admin.getToken())
        .grantRole(
            GrantRoleRequest.newBuilder()
                .setRoleName("analyst")
                .setUsername(ALICE_USERNAME)
                .build());

    // Verify alice has the analyst role
    var userDetail =
        userStub(admin.getUserId(), admin.getToken())
            .describeUserByName(
                DescribeUserByNameRequest.newBuilder().setUsername(ALICE_USERNAME).build());
    assertThat(userDetail.getUser().getRolesList()).contains("analyst");
  }

  @Test
  @Order(4)
  void listRoles_shouldShowCustomRole() {
    // Role list shows custom role
    AuthenticateWithPasswordResponse admin = authenticateAdmin();
    var response =
        roleStub(admin.getUserId(), admin.getToken())
            .listRoles(ListRolesRequest.getDefaultInstance());

    assertThat(response.getRolesList()).extracting("name").contains("SUPERADMIN", "analyst");
  }

  @Test
  @Order(5)
  void nonSuperadminCreateRole_shouldBeRejected() {
    // Non-SUPERADMIN role creation
    AuthenticateWithPasswordResponse alice = authenticate(ALICE_USERNAME, ALICE_PASSWORD);

    assertThatThrownBy(
            () ->
                roleStub(alice.getUserId(), alice.getToken())
                    .createRole(CreateRoleRequest.newBuilder().setRoleName("other-role").build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.PERMISSION_DENIED.getCode());
            });
  }

  @Test
  @Order(6)
  void deleteBuiltInRole_shouldFail() {
    // Delete built-in role
    AuthenticateWithPasswordResponse admin = authenticateAdmin();

    assertThatThrownBy(
            () ->
                roleStub(admin.getUserId(), admin.getToken())
                    .deleteRole(DeleteRoleRequest.newBuilder().setRoleName("SUPERADMIN").build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.FAILED_PRECONDITION.getCode());
            });
  }

  @Test
  @Order(7)
  void revokeRole_shouldSucceed() {
    // Role revoke
    AuthenticateWithPasswordResponse admin = authenticateAdmin();
    roleStub(admin.getUserId(), admin.getToken())
        .revokeRole(
            RevokeRoleRequest.newBuilder()
                .setRoleName("analyst")
                .setUsername(ALICE_USERNAME)
                .build());

    // Verify alice no longer has the analyst role
    var userDetail =
        userStub(admin.getUserId(), admin.getToken())
            .describeUserByName(
                DescribeUserByNameRequest.newBuilder().setUsername(ALICE_USERNAME).build());
    assertThat(userDetail.getUser().getRolesList()).doesNotContain("analyst");
  }

  @Test
  @Order(8)
  void deleteCustomRole_shouldSucceed() {
    // Delete custom role (alice cleanup is handled in @AfterAll)
    AuthenticateWithPasswordResponse admin = authenticateAdmin();
    var response =
        roleStub(admin.getUserId(), admin.getToken())
            .deleteRole(DeleteRoleRequest.newBuilder().setRoleName("analyst").build());

    assertThat(response.getDeleted()).isTrue();
  }
}
