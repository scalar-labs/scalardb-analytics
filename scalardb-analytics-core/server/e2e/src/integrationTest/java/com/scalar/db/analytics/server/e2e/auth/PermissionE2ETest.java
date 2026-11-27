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
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantCatalogPermissionRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantRoleRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.GranteeType;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListPermissionsRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.PermissionSource;
import com.scalar.db.analytics.grpc.generated.auth.v1.PermissionType;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeCatalogPermissionRequest;
import com.scalar.db.analytics.grpc.generated.catalog.v1.CreateCatalogRequest;
import com.scalar.db.analytics.grpc.generated.catalog.v1.DeleteCatalogRequest;
import com.scalar.db.analytics.grpc.generated.catalog.v1.ListAllCatalogsRequest;
import com.scalar.db.analytics.server.e2e.support.AuthE2ETestBase;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

/** E2E tests for catalog CRUD with permissions, permission management, and resource hierarchy. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PermissionE2ETest extends AuthE2ETestBase {

  private static final String ALICE_USERNAME = "alice";
  private static final String ALICE_PASSWORD = "alice-password";
  private static final String BOB_USERNAME = "bob";
  private static final String BOB_PASSWORD = "bob-password";
  private static final String TEST_CATALOG = "test_catalog";

  // --- Catalog CRUD with permissions ---

  @Test
  @Order(1)
  void adminCreateCatalog_shouldSucceed() {
    // Create catalog (admin)
    AuthenticateWithPasswordResponse admin = authenticateAdmin();
    var response =
        catalogStub(admin.getUserId(), admin.getToken())
            .createCatalog(CreateCatalogRequest.newBuilder().setCatalogName(TEST_CATALOG).build());

    assertThat(response.getCatalog().getName()).isEqualTo(TEST_CATALOG);
  }

  @Test
  @Order(2)
  void adminListCatalogs_shouldShowCatalog() {
    // List catalogs (admin)
    AuthenticateWithPasswordResponse admin = authenticateAdmin();
    var response =
        catalogStub(admin.getUserId(), admin.getToken())
            .listAllCatalogs(ListAllCatalogsRequest.getDefaultInstance());

    assertThat(response.getCatalogsList()).extracting("name").contains(TEST_CATALOG);
  }

  @Test
  @Order(3)
  void nonSuperadminCreateCatalog_shouldBeRejected() {
    // Create catalog (alice) - PERMISSION_DENIED
    AuthenticateWithPasswordResponse admin = authenticateAdmin();
    userStub(admin.getUserId(), admin.getToken())
        .createUserWithBackendUser(
            CreateUserWithBackendUserRequest.newBuilder()
                .setUsername(ALICE_USERNAME)
                .setBackendUsername(ALICE_USERNAME)
                .setPassword(ALICE_PASSWORD)
                .build());

    AuthenticateWithPasswordResponse alice = authenticate(ALICE_USERNAME, ALICE_PASSWORD);

    assertThatThrownBy(
            () ->
                catalogStub(alice.getUserId(), alice.getToken())
                    .createCatalog(
                        CreateCatalogRequest.newBuilder().setCatalogName("alice_catalog").build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.PERMISSION_DENIED.getCode());
            });
  }

  @Test
  @Order(4)
  void userWithoutPermissions_listCatalogs_shouldReturnEmpty() {
    // List catalogs (alice, no permissions) - Empty list
    AuthenticateWithPasswordResponse alice = authenticate(ALICE_USERNAME, ALICE_PASSWORD);
    var response =
        catalogStub(alice.getUserId(), alice.getToken())
            .listAllCatalogs(ListAllCatalogsRequest.getDefaultInstance());

    assertThat(response.getCatalogsList()).isEmpty();
  }

  // --- Permission management ---

  @Test
  @Order(5)
  void grantCatalogRead_shouldAllowListCatalogs() {
    // Grant CATALOG_READ to alice, then alice can list catalogs
    AuthenticateWithPasswordResponse admin = authenticateAdmin();
    permissionStub(admin.getUserId(), admin.getToken())
        .grantCatalogPermission(
            GrantCatalogPermissionRequest.newBuilder()
                .setGranteeType(GranteeType.GRANTEE_TYPE_USER)
                .setGranteeName(ALICE_USERNAME)
                .setPermission(PermissionType.PERMISSION_TYPE_CATALOG_READ)
                .setCatalogName(TEST_CATALOG)
                .build());

    AuthenticateWithPasswordResponse alice = authenticate(ALICE_USERNAME, ALICE_PASSWORD);
    var catalogs =
        catalogStub(alice.getUserId(), alice.getToken())
            .listAllCatalogs(ListAllCatalogsRequest.getDefaultInstance());

    assertThat(catalogs.getCatalogsList()).extracting("name").contains(TEST_CATALOG);
  }

  @Test
  @Order(6)
  void listPermissions_shouldShowDirectPermission() {
    // List alice's permissions
    AuthenticateWithPasswordResponse admin = authenticateAdmin();
    var response =
        permissionStub(admin.getUserId(), admin.getToken())
            .listPermissions(
                ListPermissionsRequest.newBuilder().setUsername(ALICE_USERNAME).build());

    assertThat(response.getPermissionsList())
        .anyMatch(
            p ->
                p.getPermission() == PermissionType.PERMISSION_TYPE_CATALOG_READ
                    && p.getSource() == PermissionSource.PERMISSION_SOURCE_DIRECT);
  }

  @Test
  @Order(7)
  void roleBasedPermission_shouldWork() {
    // Role-based permissions
    AuthenticateWithPasswordResponse admin = authenticateAdmin();

    // Create analyst role and grant CATALOG_READ to role
    roleStub(admin.getUserId(), admin.getToken())
        .createRole(CreateRoleRequest.newBuilder().setRoleName("analyst").build());

    permissionStub(admin.getUserId(), admin.getToken())
        .grantCatalogPermission(
            GrantCatalogPermissionRequest.newBuilder()
                .setGranteeType(GranteeType.GRANTEE_TYPE_ROLE)
                .setGranteeName("analyst")
                .setPermission(PermissionType.PERMISSION_TYPE_CATALOG_READ)
                .setCatalogName(TEST_CATALOG)
                .build());

    // Create bob and assign analyst role
    userStub(admin.getUserId(), admin.getToken())
        .createUserWithBackendUser(
            CreateUserWithBackendUserRequest.newBuilder()
                .setUsername(BOB_USERNAME)
                .setBackendUsername(BOB_USERNAME)
                .setPassword(BOB_PASSWORD)
                .build());

    roleStub(admin.getUserId(), admin.getToken())
        .grantRole(
            GrantRoleRequest.newBuilder().setRoleName("analyst").setUsername(BOB_USERNAME).build());

    // bob should see catalogs via role
    AuthenticateWithPasswordResponse bob = authenticate(BOB_USERNAME, BOB_PASSWORD);
    var catalogs =
        catalogStub(bob.getUserId(), bob.getToken())
            .listAllCatalogs(ListAllCatalogsRequest.getDefaultInstance());

    assertThat(catalogs.getCatalogsList()).extracting("name").contains(TEST_CATALOG);

    // Verify permission source is VIA_ROLE
    var permissions =
        permissionStub(admin.getUserId(), admin.getToken())
            .listPermissions(ListPermissionsRequest.newBuilder().setUsername(BOB_USERNAME).build());

    assertThat(permissions.getPermissionsList())
        .anyMatch(
            p ->
                p.getPermission() == PermissionType.PERMISSION_TYPE_CATALOG_READ
                    && p.getSource() == PermissionSource.PERMISSION_SOURCE_VIA_ROLE);
  }

  @Test
  @Order(8)
  void revokePermission_shouldRemoveAccess() {
    // Revoke permissions
    AuthenticateWithPasswordResponse admin = authenticateAdmin();

    // Revoke alice's direct CATALOG_READ
    permissionStub(admin.getUserId(), admin.getToken())
        .revokeCatalogPermission(
            RevokeCatalogPermissionRequest.newBuilder()
                .setGranteeType(GranteeType.GRANTEE_TYPE_USER)
                .setGranteeName(ALICE_USERNAME)
                .setPermission(PermissionType.PERMISSION_TYPE_CATALOG_READ)
                .setCatalogName(TEST_CATALOG)
                .build());

    // alice should no longer see the catalog
    AuthenticateWithPasswordResponse alice = authenticate(ALICE_USERNAME, ALICE_PASSWORD);
    var catalogs =
        catalogStub(alice.getUserId(), alice.getToken())
            .listAllCatalogs(ListAllCatalogsRequest.getDefaultInstance());

    assertThat(catalogs.getCatalogsList()).isEmpty();
  }

  @AfterAll
  void cleanUp() {
    // Remove resources created by this class so the shared DB stays clean for other E2E classes,
    // even if a test failed mid-scenario.
    AuthenticateWithPasswordResponse admin = authenticateAdmin();

    // Delete role (this also removes role assignments)
    try {
      roleStub(admin.getUserId(), admin.getToken())
          .deleteRole(DeleteRoleRequest.newBuilder().setRoleName("analyst").build());
    } catch (StatusRuntimeException ignored) {
      // May not exist
    }

    // Remove the created principals (cascade also drops their internal-backend credentials).
    for (String username : new String[] {ALICE_USERNAME, BOB_USERNAME}) {
      try {
        userStub(admin.getUserId(), admin.getToken())
            .deleteUser(
                DeleteUserRequest.newBuilder().setUsername(username).setCascade(true).build());
      } catch (StatusRuntimeException ignored) {
        // May not exist
      }
    }

    // Delete catalog
    try {
      catalogStub(admin.getUserId(), admin.getToken())
          .deleteCatalog(
              DeleteCatalogRequest.newBuilder()
                  .setCatalogName(TEST_CATALOG)
                  .setCascade(true)
                  .build());
    } catch (StatusRuntimeException ignored) {
      // May not exist
    }
  }
}
