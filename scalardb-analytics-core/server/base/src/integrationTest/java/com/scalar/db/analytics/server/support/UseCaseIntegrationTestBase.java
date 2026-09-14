/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.support;

import com.scalar.db.analytics.api.auth.AccessToken;
import com.scalar.db.analytics.api.auth.PasswordCredential;
import com.scalar.db.analytics.server.ScalarDbAnalyticsServerApplication;
import com.scalar.db.analytics.usecase.auth.AuthenticationUseCase;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for UseCase integration tests. Boots the Spring Boot application with auth enabled
 * (Internal Backend) and TLS. Tests inject UseCase beans directly without going through gRPC.
 *
 * <p>The PostgreSQL container is started once and shared across all test classes extending this
 * base. It is stopped automatically when the JVM shuts down.
 */
@SpringBootTest(classes = ScalarDbAnalyticsServerApplication.class)
public abstract class UseCaseIntegrationTestBase {

  protected static final String ADMIN_USERNAME = "admin";
  protected static final String ADMIN_PASSWORD = "admin-test-password";

  private static final SelfSignedCertGenerator CERT = SelfSignedCertGenerator.create();

  @SuppressWarnings("resource")
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16.4")
          .withDatabaseName("analytics_usecase_it")
          .withUsername("analytics")
          .withPassword("analytics");

  static {
    POSTGRES.start();
  }

  @Autowired protected AuthenticationUseCase authenticationUseCase;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("scalar.db.analytics.server.db.contact-points", () -> POSTGRES.getJdbcUrl());
    registry.add("scalar.db.analytics.server.db.username", () -> POSTGRES.getUsername());
    registry.add("scalar.db.analytics.server.db.password", () -> POSTGRES.getPassword());

    int catalogPort = findAvailablePort();
    registry.add("scalar.db.analytics.server.catalog.port", () -> catalogPort);
    registry.add("scalar.db.analytics.server.graceful-shutdown-delay-millis", () -> "0");

    // TLS (required when auth is enabled)
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

  /** Returns the admin user's UUID by authenticating with admin credentials. */
  protected UUID adminUserId() {
    AccessToken token =
        authenticationUseCase.authenticateWithPassword(
            new PasswordCredential(ADMIN_USERNAME, ADMIN_PASSWORD));
    return UUID.fromString(token.getUserId());
  }

  private static int findAvailablePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
