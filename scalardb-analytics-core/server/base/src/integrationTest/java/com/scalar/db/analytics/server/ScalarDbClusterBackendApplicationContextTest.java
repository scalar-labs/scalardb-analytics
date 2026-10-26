/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.server.auth.ScalarDbClusterPasswordBackend;
import com.scalar.db.analytics.server.support.SelfSignedCertGenerator;
import com.scalar.db.analytics.usecase.auth.PasswordAuthenticationBackend;
import com.scalar.db.analytics.usecase.auth.UserUseCase;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Boots the whole server with authentication enabled and the ScalarDB Cluster password backend
 * (with ACL delegation). No ScalarDB Cluster is needed: the channel to it connects lazily and
 * startup does not contact it.
 */
@SpringBootTest(classes = ScalarDbAnalyticsServerApplication.class)
@Testcontainers
// The container stops after this class, so don't leave the context cached for later classes.
@DirtiesContext
class ScalarDbClusterBackendApplicationContextTest {

  private static final SelfSignedCertGenerator CERT = SelfSignedCertGenerator.create();

  @SuppressWarnings("resource")
  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16.4")
          .withDatabaseName("analytics_cluster_backend_it")
          .withUsername("analytics")
          .withPassword("analytics");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("scalar.db.analytics.server.db.contact-points", POSTGRES::getJdbcUrl);
    registry.add("scalar.db.analytics.server.db.username", POSTGRES::getUsername);
    registry.add("scalar.db.analytics.server.db.password", POSTGRES::getPassword);
    int catalogPort = findAvailablePort();
    registry.add("scalar.db.analytics.server.catalog.port", () -> catalogPort);
    registry.add("scalar.db.analytics.server.graceful-shutdown-delay-millis", () -> "0");

    // TLS (required when auth is enabled)
    registry.add("scalar.db.analytics.server.tls.enabled", () -> true);
    registry.add("scalar.db.analytics.server.tls.cert-chain-path", CERT::certChainPath);
    registry.add("scalar.db.analytics.server.tls.private-key-path", CERT::privateKeyPath);

    // Auth - ScalarDB Cluster Backend
    registry.add("scalar.db.analytics.server.auth.enabled", () -> true);
    registry.add("scalar.db.analytics.server.auth.password.backend", () -> "scalardb-cluster");
    registry.add(
        "scalar.db.analytics.server.auth.password.scalardb-cluster.host", () -> "localhost");
    // Nothing listens here; startup must not need to reach ScalarDB Cluster.
    int clusterPort = findAvailablePort();
    registry.add(
        "scalar.db.analytics.server.auth.password.scalardb-cluster.port", () -> clusterPort);
    registry.add(
        "scalar.db.analytics.server.auth.password.scalardb-cluster.acl-delegation", () -> true);
    registry.add("scalar.db.analytics.server.auth.initial-admin-username", () -> "admin");
  }

  private static int findAvailablePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Autowired private ApplicationContext context;

  @Test
  void contextLoads() {
    assertThat(context.getBean(PasswordAuthenticationBackend.class))
        .isInstanceOf(ScalarDbClusterPasswordBackend.class);
    assertThat(context.getBean(UserUseCase.class)).isNotNull();
  }
}
