/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.grpc.generated.scalardb.cluster.rpc.v1.auth.AuthLoginGrpc;
import com.scalar.db.analytics.grpc.generated.scalardb.cluster.rpc.v1.auth.LoginResponse;
import com.scalar.db.analytics.usecase.auth.BackendAuthResult;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScalarDbClusterPasswordBackendTest {

  @Mock private AuthLoginGrpc.AuthLoginBlockingStub stub;

  private ScalarDbClusterPasswordBackend backend;

  @BeforeEach
  void setUp() {
    lenient().when(stub.withDeadlineAfter(anyLong(), any())).thenReturn(stub);
    backend = new ScalarDbClusterPasswordBackend(stub, 5000);
  }

  @Test
  void verifyCredential_shouldReturnResultWithUsernameAndTokenOnSuccess() throws Exception {
    LoginResponse response =
        LoginResponse.newBuilder()
            .setAuthToken("cluster-token")
            .setExpirationTimeMillis(System.currentTimeMillis() + 3600_000)
            .build();
    when(stub.login(any())).thenReturn(response);

    BackendAuthResult result = backend.verifyCredential("alice", "password123");

    assertThat(result.backendUserId()).isEqualTo("alice");
    assertThat(result.backendToken()).isEqualTo("cluster-token");
  }

  @Test
  void verifyCredential_shouldThrowInvalidCredentialsOnUnauthenticated() {
    when(stub.login(any())).thenThrow(new StatusRuntimeException(Status.UNAUTHENTICATED));

    assertThatThrownBy(() -> backend.verifyCredential("alice", "wrong"))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            e -> {
              AnalyticsException ae = (AnalyticsException) e;
              assertThat(ae.getErrorCode()).isEqualTo(AnalyticsErrorCode.AUTHENTICATION_FAILED);
            })
        .hasMessageContaining("Authentication failed");
  }

  @Test
  void verifyCredential_shouldThrowInvalidCredentialsOnPermissionDenied() {
    when(stub.login(any())).thenThrow(new StatusRuntimeException(Status.PERMISSION_DENIED));

    assertThatThrownBy(() -> backend.verifyCredential("alice", "wrong"))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            e -> {
              AnalyticsException ae = (AnalyticsException) e;
              assertThat(ae.getErrorCode()).isEqualTo(AnalyticsErrorCode.AUTHENTICATION_FAILED);
            });
  }

  @Test
  void verifyCredential_shouldThrowInvalidCredentialsOnInvalidArgument() {
    when(stub.login(any())).thenThrow(new StatusRuntimeException(Status.INVALID_ARGUMENT));

    assertThatThrownBy(() -> backend.verifyCredential("alice", "wrong"))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            e -> {
              AnalyticsException ae = (AnalyticsException) e;
              assertThat(ae.getErrorCode()).isEqualTo(AnalyticsErrorCode.AUTHENTICATION_FAILED);
            });
  }

  @Test
  void verifyCredential_shouldThrowClusterUnavailableOnUnavailable() {
    when(stub.login(any())).thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

    assertThatThrownBy(() -> backend.verifyCredential("alice", "password123"))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            e -> {
              AnalyticsException ae = (AnalyticsException) e;
              assertThat(ae.getErrorCode())
                  .isEqualTo(AnalyticsErrorCode.SCALARDB_CLUSTER_UNAVAILABLE);
            });
  }

  @Test
  void verifyCredential_shouldIncludeUnderlyingStatusAsCause() {
    StatusRuntimeException underlying =
        new StatusRuntimeException(Status.UNAVAILABLE.withDescription("connection refused"));
    when(stub.login(any())).thenThrow(underlying);

    assertThatThrownBy(() -> backend.verifyCredential("alice", "password123"))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            e -> {
              AnalyticsException ae = (AnalyticsException) e;
              assertThat(ae.getErrorCode())
                  .isEqualTo(AnalyticsErrorCode.SCALARDB_CLUSTER_UNAVAILABLE);
              assertThat(ae.getCause()).isSameAs(underlying);
            });
  }

  @Test
  void verifyCredential_shouldMapInternalStatusToInternalError() {
    when(stub.login(any())).thenThrow(new StatusRuntimeException(Status.INTERNAL));

    assertThatThrownBy(() -> backend.verifyCredential("alice", "password123"))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            e -> {
              AnalyticsException ae = (AnalyticsException) e;
              assertThat(ae.getErrorCode()).isEqualTo(AnalyticsErrorCode.INTERNAL_ERROR);
            });
  }

  @Test
  void verifyCredential_shouldThrowClusterUnavailableOnDeadlineExceeded() {
    when(stub.login(any())).thenThrow(new StatusRuntimeException(Status.DEADLINE_EXCEEDED));

    assertThatThrownBy(() -> backend.verifyCredential("alice", "password123"))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            e -> {
              AnalyticsException ae = (AnalyticsException) e;
              assertThat(ae.getErrorCode())
                  .isEqualTo(AnalyticsErrorCode.SCALARDB_CLUSTER_UNAVAILABLE);
            });
  }

  @Test
  void verifyCredential_shouldMapOtherStatusToInternalError() {
    when(stub.login(any()))
        .thenThrow(
            new StatusRuntimeException(Status.INTERNAL.withDescription("unexpected failure")));

    assertThatThrownBy(() -> backend.verifyCredential("alice", "password123"))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            e -> {
              AnalyticsException ae = (AnalyticsException) e;
              assertThat(ae.getErrorCode()).isEqualTo(AnalyticsErrorCode.INTERNAL_ERROR);
            });
  }

  @Test
  void getBackendType_shouldReturnScalarDbCluster() {
    assertThat(backend.getBackendType()).isEqualTo(PasswordBackendType.SCALARDB_CLUSTER);
  }
}
