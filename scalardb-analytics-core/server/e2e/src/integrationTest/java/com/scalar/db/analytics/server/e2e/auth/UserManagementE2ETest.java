/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.e2e.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.analytics.grpc.generated.auth.v1.AuthenticateWithPasswordResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.CreateUserWithBackendUserRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteUserRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.DescribeUserByNameRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListUsersRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.PasswordBackend;
import com.scalar.db.analytics.server.e2e.support.AuthE2ETestBase;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

/** E2E tests for user management. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserManagementE2ETest extends AuthE2ETestBase {

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
  }

  @Test
  @Order(1)
  void registerUser_shouldSucceed() {
    // Create a new principal together with its internal-backend credential (atomic composite).
    AuthenticateWithPasswordResponse admin = authenticateAdmin();
    var response =
        userStub(admin.getUserId(), admin.getToken())
            .createUserWithBackendUser(
                CreateUserWithBackendUserRequest.newBuilder()
                    .setUsername(ALICE_USERNAME)
                    .setBackendUsername(ALICE_USERNAME)
                    .setPassword(ALICE_PASSWORD)
                    .build());

    assertThat(response.getUserId()).isNotEmpty();
  }

  @Test
  @Order(2)
  void listUsers_shouldShowRegisteredUsers() {
    // List users
    AuthenticateWithPasswordResponse admin = authenticateAdmin();
    var response =
        userStub(admin.getUserId(), admin.getToken())
            .listUsers(ListUsersRequest.getDefaultInstance());

    assertThat(response.getUsersList())
        .extracting("username")
        .contains(ADMIN_USERNAME, ALICE_USERNAME);
  }

  @Test
  @Order(3)
  void describeUser_shouldShowUserDetails() {
    // User details
    AuthenticateWithPasswordResponse admin = authenticateAdmin();
    var response =
        userStub(admin.getUserId(), admin.getToken())
            .describeUserByName(
                DescribeUserByNameRequest.newBuilder().setUsername(ALICE_USERNAME).build());

    assertThat(response.getUser().getUsername()).isEqualTo(ALICE_USERNAME);
    assertThat(response.getUser().getBackend())
        .isEqualTo(PasswordBackend.PASSWORD_BACKEND_INTERNAL);
  }

  @Test
  @Order(4)
  void newUserLogin_shouldSucceed() {
    // New user login
    AuthenticateWithPasswordResponse aliceAuth = authenticate(ALICE_USERNAME, ALICE_PASSWORD);

    assertThat(aliceAuth.getToken()).isNotEmpty();
    assertThat(aliceAuth.getUserId()).isNotEmpty();
  }

  @Test
  @Order(5)
  void nonSuperadminRegisterUser_shouldBeRejected() {
    // Non-SUPERADMIN registration
    AuthenticateWithPasswordResponse alice = authenticate(ALICE_USERNAME, ALICE_PASSWORD);

    assertThatThrownBy(
            () ->
                userStub(alice.getUserId(), alice.getToken())
                    .createUserWithBackendUser(
                        CreateUserWithBackendUserRequest.newBuilder()
                            .setUsername("bob")
                            .setBackendUsername("bob")
                            .setPassword("bob-password")
                            .build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.PERMISSION_DENIED.getCode());
            });
  }

  @Test
  @Order(6)
  void unregisterUser_shouldSucceed() {
    // Deleting the principal with cascade removes the linked internal-backend credential
    // as part of the same transaction.
    AuthenticateWithPasswordResponse admin = authenticateAdmin();
    var response =
        userStub(admin.getUserId(), admin.getToken())
            .deleteUser(
                DeleteUserRequest.newBuilder()
                    .setUsername(ALICE_USERNAME)
                    .setCascade(true)
                    .build());

    assertThat(response.getDeleted()).isTrue();
  }

  @Test
  @Order(7)
  void loginAfterUnregistration_shouldFail() {
    // Login after unregistration
    assertThatThrownBy(() -> authenticate(ALICE_USERNAME, ALICE_PASSWORD))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.UNAUTHENTICATED.getCode());
            });
  }
}
