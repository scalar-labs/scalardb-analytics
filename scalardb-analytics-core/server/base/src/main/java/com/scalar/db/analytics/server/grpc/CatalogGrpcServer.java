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
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CatalogGrpcServer implements ManagedGrpcServer {
  private static final int WAITING_SECONDS_FOR_SHUTDOWN = 60;

  @Nullable private Server server;
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

    server = serverBuilder.build();
    server.start();
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

  @Override
  public void stop() throws InterruptedException {
    if (server != null) {
      boolean shutdown =
          server.shutdown().awaitTermination(WAITING_SECONDS_FOR_SHUTDOWN, TimeUnit.SECONDS);
      if (!shutdown) {
        logger.warn(
            "Catalog gRPC server did not shut down in {} seconds, forcing shutdown",
            WAITING_SECONDS_FOR_SHUTDOWN);
        server.shutdownNow();
      } else {
        logger.info("Catalog gRPC server shut down successfully");
      }
    }
  }

  @Override
  public void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
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
}
