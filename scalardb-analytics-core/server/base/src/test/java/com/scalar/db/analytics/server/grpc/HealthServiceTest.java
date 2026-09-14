/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HealthServiceTest {

  private HealthService healthService;
  private HealthGrpc.HealthBlockingStub blockingStub;
  private Server server;
  private ManagedChannel channel;

  @BeforeEach
  void setUp() throws IOException {
    String serverName = InProcessServerBuilder.generateName();
    healthService = new HealthService();

    server =
        InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(healthService)
            .build()
            .start();

    channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
    blockingStub = HealthGrpc.newBlockingStub(channel);
  }

  @AfterEach
  void tearDown() {
    channel.shutdownNow();
    server.shutdownNow();
  }

  @Test
  void check_ShouldReturnServingStatus() {
    // Arrange
    HealthCheckRequest request = HealthCheckRequest.newBuilder().build();

    // Act
    HealthCheckResponse response = blockingStub.check(request);

    // Assert
    assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.ServingStatus.SERVING);
  }

  @Test
  void check_AfterDecommission_ShouldReturnNotServingStatus() {
    // Arrange
    HealthCheckRequest request = HealthCheckRequest.newBuilder().build();
    healthService.decommission();

    // Act
    HealthCheckResponse response = blockingStub.check(request);

    // Assert
    assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.ServingStatus.NOT_SERVING);
  }

  @Test
  void check_WithServiceName_ShouldReturnServingStatus() {
    // Arrange
    HealthCheckRequest request =
        HealthCheckRequest.newBuilder()
            .setService("com.scalar.db.analytics.grpc.CatalogService")
            .build();

    // Act
    HealthCheckResponse response = blockingStub.check(request);

    // Assert
    assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.ServingStatus.SERVING);
  }
}
