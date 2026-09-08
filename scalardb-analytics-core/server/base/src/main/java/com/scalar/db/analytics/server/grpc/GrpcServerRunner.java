/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import com.scalar.db.analytics.server.config.ServerCommonProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Unified runner for all gRPC servers. This component manages the lifecycle of every
 * ManagedGrpcServer implementation present on the classpath.
 */
@Component
public class GrpcServerRunner {
  private static final Logger logger = LoggerFactory.getLogger(GrpcServerRunner.class);

  private final List<ManagedGrpcServer> grpcServers;
  private final ServerCommonProperties serverCommonProperties;

  public GrpcServerRunner(
      List<ManagedGrpcServer> grpcServers,
      ServerCommonProperties serverCommonProperties,
      List<ManagedServerResource> managedServerResources) {
    // managedServerResources is injected for destroy order control only:
    // Spring destroys GrpcServerRunner before the resources it depends on, so the gRPC servers
    // stop while those resources are still usable. See ManagedServerResource.
    this.grpcServers = grpcServers;
    this.serverCommonProperties = serverCommonProperties;
  }

  @PostConstruct
  public void start() throws IOException {
    for (ManagedGrpcServer server : grpcServers) {
      server.start();
      logger.info("{} started", server.getServerName());
    }
  }

  @PreDestroy
  public void stop() {
    logger.info("Shutting down all gRPC servers...");

    // Decommission all health services first
    for (ManagedGrpcServer server : grpcServers) {
      HealthService healthService = server.getHealthService();
      if (healthService != null) {
        healthService.decommission();
        logger.info("{} health service decommissioned", server.getServerName());
      }
    }

    // Sleep once to allow time for health checks to propagate
    long gracefulShutdownDelayMillis = serverCommonProperties.getGracefulShutdownDelayMillis();
    if (gracefulShutdownDelayMillis > 0) {
      try {
        logger.info("Waiting {} ms for graceful shutdown...", gracefulShutdownDelayMillis);
        Thread.sleep(gracefulShutdownDelayMillis);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        logger.warn("Graceful shutdown delay interrupted", e);
      }
    }

    // Stop all servers
    for (ManagedGrpcServer server : grpcServers) {
      try {
        server.stop();
        logger.info("{} shut down", server.getServerName());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        logger.error("Interrupted while stopping {}", server.getServerName(), e);
      }
    }
  }

  public void blockUntilShutdown() throws InterruptedException {
    // Block on the first server (typically the catalog server)
    if (!grpcServers.isEmpty()) {
      grpcServers.get(0).blockUntilShutdown();
    }
  }
}
