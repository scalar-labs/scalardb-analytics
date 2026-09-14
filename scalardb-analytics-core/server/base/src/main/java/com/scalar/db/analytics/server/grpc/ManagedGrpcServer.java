/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import java.io.IOException;
import org.jspecify.annotations.Nullable;

/**
 * Interface for gRPC servers that are managed by the unified GrpcServerRunner. Implementations are
 * discovered by injection, so a distribution adds a server by putting one on the classpath.
 */
public interface ManagedGrpcServer {

  /**
   * Starts the gRPC server.
   *
   * @throws IOException if the server fails to start
   */
  void start() throws IOException;

  /**
   * Stops the gRPC server.
   *
   * @throws InterruptedException if interrupted while waiting for shutdown
   */
  void stop() throws InterruptedException;

  /**
   * Blocks until the server is terminated.
   *
   * @throws InterruptedException if interrupted while waiting
   */
  void blockUntilShutdown() throws InterruptedException;

  /**
   * Returns the health service for this server, if available.
   *
   * @return the health service, or null if not available
   */
  @Nullable HealthService getHealthService();

  /**
   * Returns a descriptive name for this server (for logging purposes).
   *
   * @return the server name
   */
  String getServerName();
}
