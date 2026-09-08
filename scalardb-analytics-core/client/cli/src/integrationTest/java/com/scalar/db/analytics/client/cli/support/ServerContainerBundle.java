/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.support;

import com.scalar.db.analytics.api.testing.TestImages;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Manages the lifecycle of a ScalarDB Analytics Server and its PostgreSQL dependency for E2E
 * testing. Ported from the Spark module's ServerContainer.scala.
 */
public class ServerContainerBundle implements AutoCloseable {

  private static final String DEFAULT_SERVER_IMAGE =
      "ghcr.io/scalar-labs/scalardb-analytics-server:latest";
  private static final String ENV_SERVER_IMAGE = "SCALARDB_ANALYTICS_SERVER_IMAGE";

  private static DockerImageName resolveServerImage() {
    String env = System.getenv(ENV_SERVER_IMAGE);
    String imageName = (env != null && !env.trim().isEmpty()) ? env.trim() : DEFAULT_SERVER_IMAGE;
    return DockerImageName.parse(imageName);
  }

  private static final int CATALOG_PORT = 11051;
  private static final String POSTGRES_ALIAS = "postgres-db";

  private final PostgreSQLContainer<?> postgresContainer;
  private final GenericContainer<?> serverContainer;
  private final Path configDir;
  private final Network network;

  private ServerContainerBundle(
      PostgreSQLContainer<?> postgresContainer,
      GenericContainer<?> serverContainer,
      Path configDir,
      Network network) {
    this.postgresContainer = postgresContainer;
    this.serverContainer = serverContainer;
    this.configDir = configDir;
    this.network = network;
  }

  /** Returns the host for connecting to the gRPC server from the test process. */
  public String host() {
    return "localhost";
  }

  /** Returns the mapped port for the gRPC catalog service. */
  public int port() {
    return serverContainer.getMappedPort(CATALOG_PORT);
  }

  /** Returns the PostgreSQL container for direct JDBC access in tests. */
  public PostgreSQLContainer<?> postgresContainer() {
    return postgresContainer;
  }

  /**
   * Returns the internal hostname of the PostgreSQL container within the Docker network. This is
   * used for constructing provider JSON that the server can resolve.
   */
  public String postgresInternalHost() {
    return POSTGRES_ALIAS;
  }

  /** Returns the internal port of PostgreSQL within the Docker network (always 5432). */
  public int postgresInternalPort() {
    return 5432;
  }

  /** Starts all containers and returns the bundle. */
  public static ServerContainerBundle start() {
    Network network = Network.newNetwork();

    PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>(TestImages.POSTGRESQL_16)
            .withDatabaseName("scalardb_analytics")
            .withUsername("postgres")
            .withPassword("postgres")
            .withNetwork(network)
            .withNetworkAliases(POSTGRES_ALIAS);

    Path configDir = null;
    try {
      postgres.start();

      configDir = Files.createTempDirectory("scalardb-analytics-server-config-");

      Path configFile = configDir.resolve("server.properties");
      String configContent =
          String.format(
              "scalar.db.analytics.server.db.contact-points=jdbc:postgresql://%s:5432/%s\n"
                  + "scalar.db.analytics.server.db.username=%s\n"
                  + "scalar.db.analytics.server.db.password=%s\n",
              POSTGRES_ALIAS,
              postgres.getDatabaseName(),
              postgres.getUsername(),
              postgres.getPassword());
      Files.write(configFile, configContent.getBytes(StandardCharsets.UTF_8));

      WaitAllStrategy waitStrategy =
          new WaitAllStrategy()
              .withStrategy(
                  Wait.forLogMessage(".*Updated ScalarDB schema from class path resource.*", 1))
              .withStrategy(Wait.forListeningPorts(CATALOG_PORT))
              .withStartupTimeout(Duration.ofMinutes(2));

      @SuppressWarnings("resource")
      GenericContainer<?> server =
          new GenericContainer<>(resolveServerImage())
              .withExposedPorts(CATALOG_PORT)
              .withNetwork(network)
              .withCopyFileToContainer(
                  MountableFile.forHostPath(configFile.toString()),
                  "/scalardb-analytics-server/config/server.properties")
              .withExtraHost("localhost", "host-gateway")
              .withCreateContainerCmdModifier(cmd -> cmd.withUser("root"))
              .waitingFor(waitStrategy)
              .withCommand(
                  "start", "--config", "/scalardb-analytics-server/config/server.properties");
      server.start();

      // Remove default localhost entries from /etc/hosts to use host-gateway mapping only.
      // This is necessary because PostgreSQL JDBC driver resolves localhost to 127.0.0.1 first.
      org.testcontainers.containers.Container.ExecResult result =
          server.execInContainer(
              "sh",
              "-c",
              "grep -v '^127\\.0\\.0\\.1.*localhost' /etc/hosts"
                  + " | grep -v '^::1.*localhost' > /tmp/hosts"
                  + " && cat /tmp/hosts > /etc/hosts");
      if (result.getExitCode() != 0) {
        throw new RuntimeException("Failed to modify /etc/hosts: " + result.getStderr());
      }

      return new ServerContainerBundle(postgres, server, configDir, network);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      cleanupOnStartFailure(postgres, configDir, network);
      throw new RuntimeException("Interrupted during server startup", e);
    } catch (Exception e) {
      cleanupOnStartFailure(postgres, configDir, network);
      throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
    }
  }

  @SuppressWarnings("EmptyCatch")
  private static void cleanupOnStartFailure(
      PostgreSQLContainer<?> postgres, Path configDir, Network network) {
    try {
      postgres.stop();
    } catch (Exception ignored) {
      // Best-effort cleanup during startup failure; original exception will be rethrown
    }
    if (configDir != null) {
      deleteRecursively(configDir);
    }
    try {
      network.close();
    } catch (Exception ignored) {
      // Best-effort cleanup during startup failure; original exception will be rethrown
    }
  }

  @Override
  public void close() {
    RuntimeException firstException = null;

    try {
      serverContainer.stop();
    } catch (RuntimeException e) {
      firstException = e;
    }

    try {
      postgresContainer.stop();
    } catch (RuntimeException e) {
      if (firstException == null) {
        firstException = e;
      }
    }

    deleteRecursively(configDir);

    try {
      network.close();
    } catch (RuntimeException e) {
      if (firstException == null) {
        firstException = e;
      }
    }

    if (firstException != null) {
      throw firstException;
    }
  }

  private static void deleteRecursively(Path path) {
    if (!Files.exists(path)) {
      return;
    }
    try {
      Files.walkFileTree(
          path,
          new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
              Files.delete(file);
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                throws IOException {
              Files.delete(dir);
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException e) {
      // best-effort cleanup
    }
  }
}
