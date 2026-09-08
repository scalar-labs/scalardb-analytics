/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.auth;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.grpc.generated.scalardb.cluster.rpc.v1.auth.AuthLoginGrpc;
import com.scalar.db.analytics.grpc.generated.scalardb.cluster.rpc.v1.auth.LoginRequest;
import com.scalar.db.analytics.grpc.generated.scalardb.cluster.rpc.v1.auth.LoginResponse;
import com.scalar.db.analytics.usecase.auth.BackendAuthResult;
import com.scalar.db.analytics.usecase.auth.JitProvisioningBackend;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.TimeUnit;

/**
 * Password authentication backend that delegates to ScalarDB Cluster's {@code AuthLogin.Login} RPC.
 *
 * <p>On the first successful authentication, a local {@code AuthUser} and {@code PasswordIdentity}
 * are automatically created via JIT provisioning. The auth token returned by ScalarDB Cluster is
 * included in the result so it can be stored for subsequent privilege checks.
 */
public class ScalarDbClusterPasswordBackend implements JitProvisioningBackend {

  private final AuthLoginGrpc.AuthLoginBlockingStub stub;
  private final long deadlineMillis;

  public ScalarDbClusterPasswordBackend(
      AuthLoginGrpc.AuthLoginBlockingStub stub, long deadlineMillis) {
    this.stub = stub;
    this.deadlineMillis = deadlineMillis;
  }

  @Override
  public BackendAuthResult verifyCredential(String username, String password) {
    LoginRequest request =
        LoginRequest.newBuilder().setUsername(username).setPassword(password).build();

    try {
      LoginResponse response =
          stub.withDeadlineAfter(deadlineMillis, TimeUnit.MILLISECONDS).login(request);
      return new BackendAuthResult(username, response.getAuthToken());
    } catch (StatusRuntimeException e) {
      throw mapException(e);
    }
  }

  @Override
  public PasswordBackendType getBackendType() {
    return PasswordBackendType.SCALARDB_CLUSTER;
  }

  private static AnalyticsException mapException(StatusRuntimeException e) {
    Status.Code code = e.getStatus().getCode();
    return switch (code) {
      case UNAUTHENTICATED, PERMISSION_DENIED, INVALID_ARGUMENT ->
          new AnalyticsException(AnalyticsErrorCode.AUTHENTICATION_FAILED, e);
      // Only genuinely transient transport failures are retryable, consistent with
      // ScalarDbPrivilegeClientImpl.mapException(). Other (e.g. INTERNAL/UNKNOWN) cluster failures
      // are not safe to retry as if the cluster were merely unavailable.
      case UNAVAILABLE, DEADLINE_EXCEEDED ->
          new AnalyticsException(AnalyticsErrorCode.SCALARDB_CLUSTER_UNAVAILABLE, e);
      default -> new AnalyticsException(AnalyticsErrorCode.INTERNAL_ERROR, e);
    };
  }
}
