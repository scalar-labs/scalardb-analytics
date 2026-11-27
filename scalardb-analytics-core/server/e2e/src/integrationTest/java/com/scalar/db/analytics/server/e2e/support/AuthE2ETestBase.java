/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.e2e.support;

import com.scalar.db.analytics.grpc.generated.auth.v1.AuthServiceGrpc;
import com.scalar.db.analytics.grpc.generated.auth.v1.AuthenticateWithPasswordRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.AuthenticateWithPasswordResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.PermissionServiceGrpc;
import com.scalar.db.analytics.grpc.generated.auth.v1.RoleServiceGrpc;
import com.scalar.db.analytics.grpc.generated.auth.v1.UserServiceGrpc;
import com.scalar.db.analytics.grpc.generated.catalog.v1.CatalogServiceGrpc;
import com.scalar.db.analytics.grpc.generated.datasource.v1.DataSourceServiceGrpc;
import com.scalar.db.analytics.grpc.generated.namespace.v1.NamespaceServiceGrpc;
import com.scalar.db.analytics.grpc.generated.table.v1.TableServiceGrpc;
import com.scalar.db.analytics.server.ScalarDbAnalyticsServerApplication;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ForwardingClientCall;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import javax.net.ssl.SSLException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for auth E2E integration tests. Boots the full Spring Boot application with auth
 * enabled (Internal Backend) and TLS using a dynamically generated self-signed certificate.
 * Provides gRPC client stubs for all auth-related services.
 */
@SpringBootTest(classes = ScalarDbAnalyticsServerApplication.class)
public abstract class AuthE2ETestBase {

  protected static final String ADMIN_USERNAME = "admin";
  protected static final String ADMIN_PASSWORD = "admin-test-password";

  private static final SelfSignedCertGenerator CERT = SelfSignedCertGenerator.create();
  private static final int CATALOG_PORT = findAvailablePort();

  @SuppressWarnings("resource")
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16.4")
          .withDatabaseName("analytics_e2e")
          .withUsername("analytics")
          .withPassword("analytics");

  static {
    POSTGRES.start();
  }

  private static ManagedChannel channel;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    // Use lambdas for container-dependent properties so they resolve lazily
    registry.add("scalar.db.analytics.server.db.contact-points", () -> POSTGRES.getJdbcUrl());
    registry.add("scalar.db.analytics.server.db.username", () -> POSTGRES.getUsername());
    registry.add("scalar.db.analytics.server.db.password", () -> POSTGRES.getPassword());
    registry.add("scalar.db.analytics.server.catalog.port", () -> CATALOG_PORT);
    registry.add("scalar.db.analytics.server.graceful-shutdown-delay-millis", () -> "0");

    // TLS
    registry.add("scalar.db.analytics.server.tls.enabled", () -> true);
    registry.add("scalar.db.analytics.server.tls.cert-chain-path", CERT::certChainPath);
    registry.add("scalar.db.analytics.server.tls.private-key-path", CERT::privateKeyPath);

    // Auth - Internal Backend
    registry.add("scalar.db.analytics.server.auth.enabled", () -> true);
    registry.add("scalar.db.analytics.server.auth.password.backend", () -> "internal");
    registry.add("scalar.db.analytics.server.auth.initial-admin-username", () -> ADMIN_USERNAME);
    registry.add(
        "scalar.db.analytics.server.auth.password.internal.initial-admin-password",
        () -> ADMIN_PASSWORD);
  }

  @BeforeAll
  static void setUpChannel() throws SSLException {
    SslContext sslContext = GrpcSslContexts.forClient().trustManager(CERT.certFile()).build();

    channel =
        NettyChannelBuilder.forAddress("localhost", CATALOG_PORT).sslContext(sslContext).build();
  }

  @AfterAll
  static void tearDownChannel() {
    if (channel != null) {
      channel.shutdownNow();
      channel = null;
    }
  }

  // --- Service stubs (unauthenticated) ---

  protected AuthServiceGrpc.AuthServiceBlockingStub authStub() {
    return AuthServiceGrpc.newBlockingStub(channel);
  }

  protected CatalogServiceGrpc.CatalogServiceBlockingStub catalogStub() {
    return CatalogServiceGrpc.newBlockingStub(channel);
  }

  // --- Service stubs (authenticated) ---

  protected CatalogServiceGrpc.CatalogServiceBlockingStub catalogStub(String userId, String token) {
    return CatalogServiceGrpc.newBlockingStub(authenticatedChannel(userId, token));
  }

  protected DataSourceServiceGrpc.DataSourceServiceBlockingStub dataSourceStub(
      String userId, String token) {
    return DataSourceServiceGrpc.newBlockingStub(authenticatedChannel(userId, token));
  }

  protected NamespaceServiceGrpc.NamespaceServiceBlockingStub namespaceStub(
      String userId, String token) {
    return NamespaceServiceGrpc.newBlockingStub(authenticatedChannel(userId, token));
  }

  protected TableServiceGrpc.TableServiceBlockingStub tableStub(String userId, String token) {
    return TableServiceGrpc.newBlockingStub(authenticatedChannel(userId, token));
  }

  protected RoleServiceGrpc.RoleServiceBlockingStub roleStub(String userId, String token) {
    return RoleServiceGrpc.newBlockingStub(authenticatedChannel(userId, token));
  }

  protected PermissionServiceGrpc.PermissionServiceBlockingStub permissionStub(
      String userId, String token) {
    return PermissionServiceGrpc.newBlockingStub(authenticatedChannel(userId, token));
  }

  protected UserServiceGrpc.UserServiceBlockingStub userStub(String userId, String token) {
    return UserServiceGrpc.newBlockingStub(authenticatedChannel(userId, token));
  }

  // --- Helper methods ---

  /** Authenticates with the admin user and returns the response containing the token and userId. */
  protected AuthenticateWithPasswordResponse authenticateAdmin() {
    return authenticate(ADMIN_USERNAME, ADMIN_PASSWORD);
  }

  /** Authenticates with the given credentials and returns the response. */
  protected AuthenticateWithPasswordResponse authenticate(String username, String password) {
    return authStub()
        .authenticateWithPassword(
            AuthenticateWithPasswordRequest.newBuilder()
                .setUsername(username)
                .setPassword(password)
                .build());
  }

  private Channel authenticatedChannel(String userId, String token) {
    return ClientInterceptors.intercept(channel, new AuthHeaderInterceptor(userId, token));
  }

  private static int findAvailablePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Client interceptor that adds Authorization (Bearer token) and x-user-id headers. */
  private static class AuthHeaderInterceptor implements ClientInterceptor {
    private static final Metadata.Key<String> AUTHORIZATION_KEY =
        Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> USER_ID_KEY =
        Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER);

    private final String userId;
    private final String token;

    AuthHeaderInterceptor(String userId, String token) {
      this.userId = userId;
      this.token = token;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
        MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
      return new ForwardingClientCall.SimpleForwardingClientCall<>(
          next.newCall(method, callOptions)) {
        @Override
        public void start(Listener<RespT> responseListener, Metadata headers) {
          headers.put(AUTHORIZATION_KEY, "Bearer " + token);
          headers.put(USER_ID_KEY, userId);
          super.start(responseListener, headers);
        }
      };
    }
  }
}
