/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import com.scalar.db.analytics.server.config.CatalogServerProperties;
import com.scalar.db.analytics.server.config.ServerTlsProperties;
import com.scalar.db.analytics.server.config.auth.AuthServerProperties;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CatalogGrpcServer implements ManagedGrpcServer {
  private static final int WAITING_SECONDS_FOR_SHUTDOWN = 60;

  @Nullable private Running running;
  private final CatalogServerProperties catalogServerProperties;
  private final ServerTlsProperties serverTlsProperties;
  private final HealthService healthService;
  private final List<BindableService> services;
  private final AuthServerProperties authServerProperties;
  @Nullable private final AuthenticationInterceptor authenticationInterceptor;

  public CatalogGrpcServer(
      CatalogServerProperties catalogServerProperties,
      ServerTlsProperties serverTlsProperties,
      HealthService healthService,
      List<BindableService> services,
      AuthServerProperties authServerProperties,
      Optional<AuthenticationInterceptor> authenticationInterceptor) {
    this.catalogServerProperties = catalogServerProperties;
    this.serverTlsProperties = serverTlsProperties;
    this.healthService = healthService;
    this.services = services;
    this.authServerProperties = authServerProperties;
    this.authenticationInterceptor = authenticationInterceptor.orElse(null);
  }

  @Override
  public void start() throws IOException {
    ServerBuilder<?> serverBuilder = ServerBuilder.forPort(catalogServerProperties.getPort());

    if (serverTlsProperties.isEnabled()) {
      logger.info("Starting Catalog gRPC server with TLS enabled");
      setTransportSecurityConfig(serverBuilder);
    } else {
      logger.info("Starting Catalog gRPC server without TLS (plaintext)");
    }

    // gRPC executes interceptors in reverse order of addition (last added = first executed).
    // ExceptionHandlingInterceptor must be the outermost so that it catches exceptions from
    // all inner interceptors, including AuthenticationInterceptor.
    if (authServerProperties.isEnabled()) {
      serverBuilder.intercept(Objects.requireNonNull(authenticationInterceptor));
    }
    serverBuilder.intercept(new ExceptionHandlingInterceptor());
    for (BindableService service : services) {
      serverBuilder.addService(service);
    }

    // The server never shuts down an executor it is given, and it does not wait for handlers that
    // are still running on it after a forced shutdown. Owning the executor lets stop() wait for
    // them before Spring destroys the resources they use.
    ExecutorService executor = newCallExecutor();
    serverBuilder.executor(executor);
    Server server = serverBuilder.build();
    try {
      server.start();
    } catch (IOException | RuntimeException e) {
      executor.shutdownNow();
      throw e;
    }
    running = new Running(server, executor);
    logger.info("Catalog gRPC server started on port {}", catalogServerProperties.getPort());
  }

  private void setTransportSecurityConfig(ServerBuilder<?> serverBuilder) throws IOException {
    // certChainPath and privateKeyPath are not null if TLS is enabled
    File certChainFile = new File(Objects.requireNonNull(serverTlsProperties.getCertChainPath()));
    File privateKeyFile = new File(Objects.requireNonNull(serverTlsProperties.getPrivateKeyPath()));

    if (!certChainFile.exists()) {
      throw new IOException("Certificate chain file not found: " + certChainFile.getAbsolutePath());
    }
    if (!privateKeyFile.exists()) {
      throw new IOException("Private key file not found: " + privateKeyFile.getAbsolutePath());
    }

    serverBuilder.useTransportSecurity(certChainFile, privateKeyFile);
  }

  /** Matches the daemon, cached thread pool that gRPC uses when no executor is given. */
  static ExecutorService newCallExecutor() {
    AtomicInteger threadCount = new AtomicInteger();
    ThreadFactory threadFactory =
        runnable -> {
          Thread thread =
              new Thread(runnable, "catalog-grpc-executor-" + threadCount.getAndIncrement());
          thread.setDaemon(true);
          return thread;
        };
    return Executors.newCachedThreadPool(threadFactory);
  }

  @Override
  public void stop() throws InterruptedException {
    Running current = running;
    if (current != null) {
      shutdownAndAwaitTermination(
          current.server(), current.executor(), WAITING_SECONDS_FOR_SHUTDOWN, TimeUnit.SECONDS);
    }
  }

  /**
   * Shuts down {@code server} and then {@code executor}, the call executor given to it.
   *
   * <p>After {@link Server#shutdownNow()}, the server reports termination as soon as its transports
   * close, while cancelled handlers may still be running on the executor. Waiting for the executor
   * is what makes sure no handler is running when this method returns normally.
   */
  static void shutdownAndAwaitTermination(
      Server server, ExecutorService executor, long timeout, TimeUnit unit)
      throws InterruptedException {
    try {
      awaitServerTermination(server, timeout, unit);
      awaitHandlerCompletion(executor, timeout, unit);
    } catch (InterruptedException e) {
      // Nothing waits for the server after this, so it must not keep running.
      server.shutdownNow();
      executor.shutdownNow();
      throw e;
    }
  }

  private static void awaitServerTermination(Server server, long timeout, TimeUnit unit)
      throws InterruptedException {
    if (server.shutdown().awaitTermination(timeout, unit)) {
      logger.info("Catalog gRPC server shut down successfully");
      return;
    }
    logger.warn("Catalog gRPC server did not shut down in {} {}, forcing shutdown", timeout, unit);
    server.shutdownNow();
    if (server.awaitTermination(timeout, unit)) {
      logger.info("Catalog gRPC server shut down after forcing shutdown");
    } else {
      logger.warn(
          "Catalog gRPC server did not terminate in {} {} after forcing shutdown", timeout, unit);
    }
  }

  private static void awaitHandlerCompletion(ExecutorService executor, long timeout, TimeUnit unit)
      throws InterruptedException {
    executor.shutdown();
    if (!executor.awaitTermination(timeout, unit)) {
      logger.warn(
          "Catalog gRPC call handlers did not finish in {} {}, interrupting them", timeout, unit);
      executor.shutdownNow();
    }
  }

  @Override
  public void blockUntilShutdown() throws InterruptedException {
    Running current = running;
    if (current != null) {
      current.server().awaitTermination();
    }
  }

  @Override
  @Nullable
  public HealthService getHealthService() {
    return healthService;
  }

  @Override
  public String getServerName() {
    return "Catalog gRPC server";
  }

  private record Running(Server server, ExecutorService executor) {}
}
