/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.grpc.generated.auth.v1.CreateInternalBackendUserRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteInternalBackendUserRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.InternalUserDirectoryServiceGrpc;
import com.scalar.db.analytics.grpc.generated.error.v1.AnalyticsError;
import com.scalar.db.analytics.service.authz.AuthorizationService;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UnsupportedInternalUserDirectoryServiceImplTest {

  private static final UUID USER_ID = UUID.randomUUID();

  private final AuthorizationService authorizationService = mock(AuthorizationService.class);
  private Server server;
  private ManagedChannel channel;
  private InternalUserDirectoryServiceGrpc.InternalUserDirectoryServiceBlockingStub stub;

  @BeforeEach
  void setUp() throws IOException {
    String serverName = "test-server-" + UUID.randomUUID();
    server =
        InProcessServerBuilder.forName(serverName)
            .addService(
                new UnsupportedInternalUserDirectoryServiceImpl(
                    PasswordBackendType.SCALARDB_CLUSTER, authorizationService))
            // The last interceptor added runs first, as in CatalogGrpcServer.
            .intercept(userIdInterceptor())
            .intercept(new ExceptionHandlingInterceptor())
            .build()
            .start();
    channel = InProcessChannelBuilder.forName(serverName).build();
    stub = InternalUserDirectoryServiceGrpc.newBlockingStub(channel);
    when(authorizationService.authorizeSuperAdmin(USER_ID)).thenReturn(true);
  }

  @AfterEach
  void tearDown() {
    channel.shutdownNow();
    server.shutdownNow();
  }

  @Test
  void createInternalBackendUser_shouldFailWithInternalPasswordBackendRequired() {
    assertInternalPasswordBackendRequired(
        () ->
            stub.createInternalBackendUser(
                CreateInternalBackendUserRequest.newBuilder()
                    .setUsername("alice")
                    .setPassword("pw")
                    .build()));
  }

  @Test
  void deleteInternalBackendUser_shouldFailWithInternalPasswordBackendRequired() {
    assertInternalPasswordBackendRequired(
        () ->
            stub.deleteInternalBackendUser(
                DeleteInternalBackendUserRequest.newBuilder().setUsername("alice").build()));
  }

  @Test
  void createInternalBackendUser_whenNotSuperAdmin_shouldFailWithAccessDenied() {
    // Same order as the internal backend: callers without the privilege do not learn the
    // configured backend.
    when(authorizationService.authorizeSuperAdmin(USER_ID)).thenReturn(false);

    assertThatThrownBy(
            () ->
                stub.createInternalBackendUser(
                    CreateInternalBackendUserRequest.newBuilder()
                        .setUsername("alice")
                        .setPassword("pw")
                        .build()))
        .isInstanceOfSatisfying(
            StatusRuntimeException.class,
            sre -> {
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.Code.PERMISSION_DENIED);
              assertThat(ExceptionHandlingInterceptorTest.analyticsErrorOf(sre).getErrorCode())
                  .isEqualTo(AnalyticsErrorCode.ACCESS_DENIED.getCode());
            });
  }

  private static ServerInterceptor userIdInterceptor() {
    return new ServerInterceptor() {
      @Override
      public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
          ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        Context ctx =
            Context.current().withValue(AuthenticationInterceptor.AUTHENTICATED_USER_ID, USER_ID);
        return Contexts.interceptCall(ctx, call, headers, next);
      }
    };
  }

  private static void assertInternalPasswordBackendRequired(ThrowingCallable call) {
    assertThatThrownBy(call)
        .isInstanceOfSatisfying(
            StatusRuntimeException.class,
            sre -> {
              // A descriptive, structured error instead of a bare UNIMPLEMENTED.
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.Code.FAILED_PRECONDITION);
              AnalyticsError error = ExceptionHandlingInterceptorTest.analyticsErrorOf(sre);
              assertThat(error.getErrorCode())
                  .isEqualTo(AnalyticsErrorCode.INTERNAL_PASSWORD_BACKEND_REQUIRED.getCode());
              assertThat(error.getMetadataMap())
                  .isEqualTo(Map.of("password_backend", "scalardb_cluster"));
            });
  }
}
