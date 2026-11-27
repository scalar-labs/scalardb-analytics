/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.e2e.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.analytics.grpc.generated.auth.v1.AuthenticateWithPasswordRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.AuthenticateWithPasswordResponse;
import com.scalar.db.analytics.grpc.generated.catalog.v1.ListAllCatalogsRequest;
import com.scalar.db.analytics.server.e2e.support.AuthE2ETestBase;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;

/** E2E tests for the authentication flow. */
class AuthenticationE2ETest extends AuthE2ETestBase {

  @Test
  void normalLogin_shouldReturnTokenAndAllowApiAccess() {
    // Normal login
    AuthenticateWithPasswordResponse response = authenticateAdmin();

    assertThat(response.getToken()).isNotEmpty();
    assertThat(response.getUserId()).isNotEmpty();
    assertThat(response.getExpiresAt()).isGreaterThan(0);

    // Verify the token works for API calls
    var catalogs =
        catalogStub(response.getUserId(), response.getToken())
            .listAllCatalogs(ListAllCatalogsRequest.getDefaultInstance());
    assertThat(catalogs).isNotNull();
  }

  @Test
  void wrongPassword_shouldFailAuthentication() {
    // Wrong password
    assertThatThrownBy(
            () ->
                authStub()
                    .authenticateWithPassword(
                        AuthenticateWithPasswordRequest.newBuilder()
                            .setUsername("admin")
                            .setPassword("wrong-password")
                            .build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.UNAUTHENTICATED.getCode());
            });
  }

  @Test
  void nonExistentUser_shouldFailAuthentication() {
    // Non-existent user
    assertThatThrownBy(
            () ->
                authStub()
                    .authenticateWithPassword(
                        AuthenticateWithPasswordRequest.newBuilder()
                            .setUsername("nonexistent")
                            .setPassword("password")
                            .build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.UNAUTHENTICATED.getCode());
            });
  }

  @Test
  void invalidToken_shouldBeRejected() {
    // Request with an invalid token should be rejected
    assertThatThrownBy(
            () ->
                catalogStub("invalid-user-id", "invalid-token")
                    .listAllCatalogs(ListAllCatalogsRequest.getDefaultInstance()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.UNAUTHENTICATED.getCode());
            });
  }

  @Test
  void missingCredentials_shouldBeRejected() {
    // Request without any auth headers should be rejected
    assertThatThrownBy(
            () -> catalogStub().listAllCatalogs(ListAllCatalogsRequest.getDefaultInstance()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.UNAUTHENTICATED.getCode());
            });
  }
}
