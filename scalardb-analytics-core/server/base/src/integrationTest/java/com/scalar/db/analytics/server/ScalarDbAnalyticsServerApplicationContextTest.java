/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = ScalarDbAnalyticsServerApplication.class)
@Testcontainers
class ScalarDbAnalyticsServerApplicationContextTest {

  @SuppressWarnings("resource")
  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16.4")
          .withDatabaseName("analytics_it")
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
    assertThat(context).isNotNull();
  }
}
