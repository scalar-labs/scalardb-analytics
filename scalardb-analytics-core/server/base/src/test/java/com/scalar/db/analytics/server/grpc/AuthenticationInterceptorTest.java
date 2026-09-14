/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.grpc.generated.auth.v1.AuthServiceGrpc;
import com.scalar.db.analytics.grpc.generated.auth.v1.AuthenticateWithPasswordRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.AuthenticateWithPasswordResponse;
import com.scalar.db.analytics.grpc.generated.catalog.v1.Catalog;
import com.scalar.db.analytics.grpc.generated.catalog.v1.CatalogServiceGrpc;
import com.scalar.db.analytics.grpc.generated.catalog.v1.CreateCatalogRequest;
import com.scalar.db.analytics.grpc.generated.catalog.v1.CreateCatalogResponse;
import com.scalar.db.analytics.usecase.auth.TokenValidationUseCase;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ForwardingClientCall;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticationInterceptorTest {

  @Mock private TokenValidationUseCase tokenValidationUseCase;

  private Server server;
  private ManagedChannel channel;
  private CatalogServiceGrpc.CatalogServiceBlockingStub catalogStub;
  private AuthServiceGrpc.AuthServiceBlockingStub authStub;
  private HealthGrpc.HealthBlockingStub healthStub;

  @BeforeEach
  void setUp() throws IOException {
    String serverName = InProcessServerBuilder.generateName();

    server =
        InProcessServerBuilder.forName(serverName)
            .intercept(new AuthenticationInterceptor(tokenValidationUseCase))
            .intercept(new ExceptionHandlingInterceptor())
            .addService(new TestCatalogServiceImpl())
            .addService(new TestAuthServiceImpl())
            .addService(new HealthService())
            .build()
            .start();

    channel = InProcessChannelBuilder.forName(serverName).build();
    catalogStub = CatalogServiceGrpc.newBlockingStub(channel);
    authStub = AuthServiceGrpc.newBlockingStub(channel);
    healthStub = HealthGrpc.newBlockingStub(channel);
  }

  @AfterEach
  void tearDown() {
    if (channel != null) {
      channel.shutdownNow();
    }
    if (server != null) {
      server.shutdownNow();
    }
  }

  @Test
  void authServiceRequest_shouldSucceedWithoutAuthHeader() {
    AuthenticateWithPasswordRequest request =
        AuthenticateWithPasswordRequest.newBuilder()
            .setUsername("alice")
            .setPassword("secret")
            .build();

    AuthenticateWithPasswordResponse response = authStub.authenticateWithPassword(request);

    assertThat(response).isNotNull();
  }

  @Test
  void protectedEndpoint_shouldFailWithoutAnyHeaders() {
    CreateCatalogRequest request = CreateCatalogRequest.newBuilder().setCatalogName("test").build();

    assertThatThrownBy(() -> catalogStub.createCatalog(request))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.UNAUTHENTICATED.getCode());
              assertThat(sre.getStatus().getDescription())
                  .contains("Authentication token is invalid");
            });
  }

  @Test
  void protectedEndpoint_shouldFailWithInvalidUserIdFormat() {
    Channel authChannel =
        ClientInterceptors.intercept(
            channel, new AuthHeaderInterceptor("Bearer token", "not-a-uuid"));
    CatalogServiceGrpc.CatalogServiceBlockingStub stubWithAuth =
        CatalogServiceGrpc.newBlockingStub(authChannel);

    CreateCatalogRequest request = CreateCatalogRequest.newBuilder().setCatalogName("test").build();

    assertThatThrownBy(() -> stubWithAuth.createCatalog(request))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.UNAUTHENTICATED.getCode());
              assertThat(sre.getStatus().getDescription())
                  .contains("Authentication token is invalid");
            });
  }

  @Test
  void protectedEndpoint_shouldFailWithoutAuthHeader() {
    UUID userId = UUID.randomUUID();
    // Send only x-user-id without Authorization header
    Channel userIdOnlyChannel =
        ClientInterceptors.intercept(
            channel,
            new ClientInterceptor() {
              @Override
              public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                  MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
                return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                    next.newCall(method, callOptions)) {
                  @Override
                  public void start(Listener<RespT> responseListener, Metadata headers) {
                    headers.put(AuthenticationInterceptor.USER_ID_HEADER, userId.toString());
                    super.start(responseListener, headers);
                  }
                };
              }
            });
    CatalogServiceGrpc.CatalogServiceBlockingStub stubWithAuth =
        CatalogServiceGrpc.newBlockingStub(userIdOnlyChannel);

    CreateCatalogRequest request = CreateCatalogRequest.newBuilder().setCatalogName("test").build();

    assertThatThrownBy(() -> stubWithAuth.createCatalog(request))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.UNAUTHENTICATED.getCode());
              assertThat(sre.getStatus().getDescription())
                  .contains("Authentication token is invalid");
            });
  }

  @Test
  void protectedEndpoint_shouldFailWithInvalidHeaderFormat() {
    UUID userId = UUID.randomUUID();
    Channel authChannel =
        ClientInterceptors.intercept(
            channel, new AuthHeaderInterceptor("InvalidFormat token123", userId.toString()));
    CatalogServiceGrpc.CatalogServiceBlockingStub stubWithAuth =
        CatalogServiceGrpc.newBlockingStub(authChannel);

    CreateCatalogRequest request = CreateCatalogRequest.newBuilder().setCatalogName("test").build();

    assertThatThrownBy(() -> stubWithAuth.createCatalog(request))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.UNAUTHENTICATED.getCode());
              assertThat(sre.getStatus().getDescription())
                  .contains("Authentication token is invalid");
            });
  }

  @Test
  void protectedEndpoint_shouldFailWithEmptyBearerToken() {
    UUID userId = UUID.randomUUID();
    Channel authChannel =
        ClientInterceptors.intercept(
            channel, new AuthHeaderInterceptor("Bearer ", userId.toString()));
    CatalogServiceGrpc.CatalogServiceBlockingStub stubWithAuth =
        CatalogServiceGrpc.newBlockingStub(authChannel);

    CreateCatalogRequest request = CreateCatalogRequest.newBuilder().setCatalogName("test").build();

    assertThatThrownBy(() -> stubWithAuth.createCatalog(request))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.UNAUTHENTICATED.getCode());
              assertThat(sre.getStatus().getDescription())
                  .contains("Authentication token is invalid");
            });
  }

  @Test
  void protectedEndpoint_shouldFailWithWhitespaceOnlyBearerToken() {
    UUID userId = UUID.randomUUID();
    Channel authChannel =
        ClientInterceptors.intercept(
            channel, new AuthHeaderInterceptor("Bearer    ", userId.toString()));
    CatalogServiceGrpc.CatalogServiceBlockingStub stubWithAuth =
        CatalogServiceGrpc.newBlockingStub(authChannel);

    CreateCatalogRequest request = CreateCatalogRequest.newBuilder().setCatalogName("test").build();

    assertThatThrownBy(() -> stubWithAuth.createCatalog(request))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.UNAUTHENTICATED.getCode());
              assertThat(sre.getStatus().getDescription())
                  .contains("Authentication token is invalid");
            });
  }

  @Test
  void protectedEndpoint_shouldFailWithInvalidToken() throws Exception {
    UUID userId = UUID.randomUUID();
    when(tokenValidationUseCase.validateToken(userId, "invalid-token"))
        .thenThrow(new AnalyticsException(AnalyticsErrorCode.TOKEN_INVALID));

    Channel authChannel =
        ClientInterceptors.intercept(
            channel, new AuthHeaderInterceptor("Bearer invalid-token", userId.toString()));
    CatalogServiceGrpc.CatalogServiceBlockingStub stubWithAuth =
        CatalogServiceGrpc.newBlockingStub(authChannel);

    CreateCatalogRequest request = CreateCatalogRequest.newBuilder().setCatalogName("test").build();

    assertThatThrownBy(() -> stubWithAuth.createCatalog(request))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.UNAUTHENTICATED.getCode());
              assertThat(sre.getStatus().getDescription())
                  .contains("Authentication token is invalid");
            });
  }

  @Test
  void protectedEndpoint_shouldSucceedWithValidToken() throws Exception {
    UUID userId = UUID.randomUUID();
    when(tokenValidationUseCase.validateToken(userId, "valid-token")).thenReturn(userId);

    Channel authChannel =
        ClientInterceptors.intercept(
            channel, new AuthHeaderInterceptor("Bearer valid-token", userId.toString()));
    CatalogServiceGrpc.CatalogServiceBlockingStub stubWithAuth =
        CatalogServiceGrpc.newBlockingStub(authChannel);

    CreateCatalogRequest request = CreateCatalogRequest.newBuilder().setCatalogName("test").build();

    CreateCatalogResponse response = stubWithAuth.createCatalog(request);

    assertThat(response).isNotNull();
  }

  @Test
  void protectedEndpoint_shouldPropagateUserIdInContext() throws Exception {
    UUID userId = UUID.randomUUID();
    when(tokenValidationUseCase.validateToken(userId, "valid-token")).thenReturn(userId);

    Channel authChannel =
        ClientInterceptors.intercept(
            channel, new AuthHeaderInterceptor("Bearer valid-token", userId.toString()));
    CatalogServiceGrpc.CatalogServiceBlockingStub stubWithAuth =
        CatalogServiceGrpc.newBlockingStub(authChannel);

    // The test service echoes the userId from Context into the catalog name
    CreateCatalogRequest request =
        CreateCatalogRequest.newBuilder().setCatalogName("capture-user-id").build();

    CreateCatalogResponse response = stubWithAuth.createCatalog(request);

    assertThat(response).isNotNull();
    assertThat(response.getCatalog().getId()).isEqualTo(userId.toString());
  }

  @Test
  void healthServiceRequest_shouldSucceedWithoutAuthHeader() {
    HealthCheckResponse response = healthStub.check(HealthCheckRequest.getDefaultInstance());

    assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.ServingStatus.SERVING);
  }

  @Test
  void protectedEndpoint_shouldReturnUnavailableWhenDatabaseConnectionFails() throws Exception {
    UUID userId = UUID.randomUUID();
    when(tokenValidationUseCase.validateToken(userId, "valid-token"))
        .thenThrow(new AnalyticsException(AnalyticsErrorCode.ANALYTICS_DB_CONNECTION_FAILED));

    Channel authChannel =
        ClientInterceptors.intercept(
            channel, new AuthHeaderInterceptor("Bearer valid-token", userId.toString()));
    CatalogServiceGrpc.CatalogServiceBlockingStub stubWithAuth =
        CatalogServiceGrpc.newBlockingStub(authChannel);

    CreateCatalogRequest request = CreateCatalogRequest.newBuilder().setCatalogName("test").build();

    assertThatThrownBy(() -> stubWithAuth.createCatalog(request))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.UNAVAILABLE.getCode());
              assertThat(sre.getStatus().getDescription())
                  .contains("Analytics database connection failed");
            });
  }

  /** Client interceptor that adds Authorization and x-user-id headers to outgoing requests. */
  private static class AuthHeaderInterceptor implements ClientInterceptor {
    private final String authHeaderValue;
    @Nullable private final String userIdHeaderValue;

    AuthHeaderInterceptor(String authHeaderValue) {
      this(authHeaderValue, null);
    }

    AuthHeaderInterceptor(String authHeaderValue, @Nullable String userIdHeaderValue) {
      this.authHeaderValue = authHeaderValue;
      this.userIdHeaderValue = userIdHeaderValue;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
        MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
      return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
          next.newCall(method, callOptions)) {
        @Override
        public void start(Listener<RespT> responseListener, Metadata headers) {
          headers.put(AuthenticationInterceptor.AUTHORIZATION_HEADER, authHeaderValue);
          if (userIdHeaderValue != null) {
            headers.put(AuthenticationInterceptor.USER_ID_HEADER, userIdHeaderValue);
          }
          super.start(responseListener, headers);
        }
      };
    }
  }

  /**
   * Test CatalogService that returns success. When catalog name is "capture-user-id", echoes the
   * authenticated user ID from Context into the Catalog.id field.
   */
  private static class TestCatalogServiceImpl extends CatalogServiceGrpc.CatalogServiceImplBase {
    @Override
    public void createCatalog(
        CreateCatalogRequest request, StreamObserver<CreateCatalogResponse> responseObserver) {
      CreateCatalogResponse.Builder builder = CreateCatalogResponse.newBuilder();
      if ("capture-user-id".equals(request.getCatalogName())) {
        UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
        if (userId != null) {
          builder.setCatalog(Catalog.newBuilder().setId(userId.toString()).build());
        }
      }
      responseObserver.onNext(builder.build());
      responseObserver.onCompleted();
    }
  }

  /** Test AuthService that returns a dummy response (used to verify whitelist). */
  private static class TestAuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {
    @Override
    public void authenticateWithPassword(
        AuthenticateWithPasswordRequest request,
        StreamObserver<AuthenticateWithPasswordResponse> responseObserver) {
      responseObserver.onNext(AuthenticateWithPasswordResponse.getDefaultInstance());
      responseObserver.onCompleted();
    }
  }
}
