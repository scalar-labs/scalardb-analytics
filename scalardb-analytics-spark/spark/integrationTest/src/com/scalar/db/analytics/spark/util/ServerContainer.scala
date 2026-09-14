/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.util

import com.scalar.db.analytics.sdk.ScalarDbAnalyticsClient
import com.scalar.db.analytics.spark.util.TestContainers.{GenericContainer, PostgreSQLContainer}
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.{Wait, WaitAllStrategy}
import org.testcontainers.utility.{DockerImageName, MountableFile}

import java.nio.file.{Files, Path}
import java.time.Duration

/** Server container bundle holding all resources created during startup.
  *
  * All resources in this bundle are created together and should be cleaned up together.
  */
case class ServerContainerBundle(
    postgresContainer: PostgreSQLContainer,
    serverContainer: GenericContainer,
    configDir: Path,
    network: Network,
    analyticsClient: ScalarDbAnalyticsClient
)

/** Trait for managing ScalarDB Analytics Server container lifecycle in tests.
  *
  * This trait provides functionality to:
  *   - Start PostgreSQL and server containers with automatic configuration
  *   - Generate server.properties dynamically
  *   - Wait for the server to be ready (health check)
  *   - Get the gRPC endpoint for Spark catalog configuration
  *
  * Usage: Extend this trait and call startServerBundle() in beforeAll()
  */
trait ServerContainer {
  private var _bundle: Option[ServerContainerBundle] = None

  protected val SERVER_IMAGE_NAME: String =
    Option(System.getenv("SCALARDB_ANALYTICS_SERVER_IMAGE"))
      .map(_.trim)
      .filter(_.nonEmpty)
      .getOrElse("ghcr.io/scalar-labs/scalardb-analytics-server:latest")
  protected val SERVER_IMAGE: DockerImageName = DockerImageName.parse(SERVER_IMAGE_NAME)
  protected val CATALOG_PORT: Int             = 11051
  private val POSTGRES_ALIAS                  = "postgres-db"

  /** Set to true to keep the server container running after tests (for debugging). Default: false
    */
  protected var keepServerContainerAlive: Boolean = false

  /** Returns the ScalarDB Analytics client for interacting with the server.
    */
  protected def analyticsClient: ScalarDbAnalyticsClient =
    _bundle
      .map(_.analyticsClient)
      .getOrElse(
        throw new IllegalStateException("Analytics client is not initialized")
      )

  /** Returns the gRPC endpoint for connecting to the server. Format: "localhost:PORT"
    */
  protected def serverEndpoint: String =
    _bundle
      .map { b =>
        val mappedPort = b.serverContainer.mappedPort(CATALOG_PORT)
        s"localhost:${mappedPort.toString}"
      }
      .getOrElse(
        throw new IllegalStateException("Server container is not started")
      )

  /** Returns the Docker network used by containers. Useful for adding other containers to the same
    * network.
    */
  protected def network: Network =
    _bundle
      .map(_.network)
      .getOrElse(
        throw new IllegalStateException("Network is not initialized")
      )

  /** Starts PostgreSQL and server containers with automatic database configuration. This method
    * sets up:
    *   - A shared Docker network for container-to-container communication
    *   - PostgreSQL container for server metadata storage
    *   - Server container configured to connect to PostgreSQL
    *
    * @param serverConfig
    *   Additional server configuration properties (default: empty)
    */
  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  protected def startServerBundle(
      serverConfig: Map[String, String] = Map.empty[String, String]
  ): Unit = {
    // Create a shared network for container communication
    val network = Network.newNetwork()

    // Start PostgreSQL container
    val postgres: PostgreSQLContainer = new PostgreSQLContainer(TestImages.POSTGRESQL_16)
      .withDatabaseName("scalardb_analytics")
      .withUsername("postgres")
      .withPassword("postgres")
      .withNetwork(network)
      .withNetworkAliases(POSTGRES_ALIAS)
    postgres.start()

    // Create a temporary directory for server configuration
    val configDir = Files.createTempDirectory("scalardb-analytics-server-config-")

    // Generate server.properties file with PostgreSQL connection
    val configFile = configDir.resolve("server.properties")
    val dbConfig = Map(
      "scalar.db.analytics.server.db.contact-points" -> s"jdbc:postgresql://$POSTGRES_ALIAS:5432/${postgres.databaseName}",
      "scalar.db.analytics.server.db.username" -> postgres.username,
      "scalar.db.analytics.server.db.password" -> postgres.password
    )
    val allConfig = serverConfig ++ dbConfig
    val configContent = allConfig
      .map { case (key, value) =>
        s"$key=$value"
      }
      .mkString("\n")
    Files.write(configFile, configContent.getBytes("UTF-8"))

    // Create and start server container
    // Wait for both gRPC ports to open AND migration to complete
    // TODO: Replace migration log with a proper "server ready" log message when available
    val waitStrategy = new WaitAllStrategy()
      .withStrategy(Wait.forLogMessage(".*Updated ScalarDB schema from class path resource.*", 1))
      .withStrategy(Wait.forListeningPorts(CATALOG_PORT))
      .withStartupTimeout(Duration.ofMinutes(2))

    val container = new GenericContainer(SERVER_IMAGE)
      .withExposedPorts(CATALOG_PORT)
      .withNetwork(network)
      .withCopyFileToContainer(
        MountableFile.forHostPath(configFile.toString),
        "/scalardb-analytics-server/config/server.properties"
      )
      // Map localhost to host gateway so server can access host's localhost
      .withExtraHost("localhost", "host-gateway")
      // Run as root to allow /etc/hosts modification
      .withCreateContainerCmdModifier { cmd =>
        val _ = cmd.withUser("root")
      }
      .waitingFor(waitStrategy)
      .withCommand(
        // Use start command with the config file
        "start",
        "--config",
        "/scalardb-analytics-server/config/server.properties"
      )
    container.start()

    // Remove default localhost entries from /etc/hosts to use host-gateway mapping only
    // This is necessary because PostgreSQL JDBC driver resolves localhost to 127.0.0.1 first
    // Use cat instead of mv because /etc/hosts is mounted by Docker and cannot be renamed
    val removeResult = container.execInContainer(
      "sh",
      "-c",
      "grep -v '^127\\.0\\.0\\.1.*localhost' /etc/hosts | grep -v '^::1.*localhost' > /tmp/hosts && cat /tmp/hosts > /etc/hosts"
    )
    if (removeResult.getExitCode != 0) {
      throw new RuntimeException(s"Failed to modify /etc/hosts: ${removeResult.getStderr}")
    }

    // Create an analytics client
    val mappedPort = container.mappedPort(CATALOG_PORT)
    val client     = ScalarDbAnalyticsClient.builder().host("localhost").port(mappedPort).build()

    // Store all resources together in a bundle
    _bundle = Some(
      ServerContainerBundle(
        postgresContainer = postgres,
        serverContainer = container,
        configDir = configDir,
        network = network,
        analyticsClient = client
      )
    )
  }

  /** Stops PostgreSQL and server containers and cleans up resources. If keepServerContainerAlive is
    * true, the server container will not be stopped (for debugging).
    */
  protected def stopServerBundle(): Unit = {
    _bundle.foreach { b =>
      // Close analytics client
      b.analyticsClient.close()

      // Stop server container (unless keepServerContainerAlive is true)
      if (!keepServerContainerAlive) {
        b.serverContainer.stop()
      }

      // Stop PostgreSQL container
      b.postgresContainer.stop()

      // Clean up config directory
      deleteRecursively(b.configDir)

      // Close network
      b.network.close()
    }
    _bundle = None
  }

  private def deleteRecursively(path: Path): Unit = {
    import java.util.Comparator
    if (Files.exists(path)) {
      Files.walk(path).sorted(Comparator.reverseOrder()).forEach(Files.delete(_))
    }
  }
}
