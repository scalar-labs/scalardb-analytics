/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.client.cli.CliRoot;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import picocli.CommandLine;

/**
 * Base class for CLI E2E tests. Starts a real ScalarDB Analytics Server via TestContainers and
 * executes CLI commands in-process against it.
 */
public abstract class CliE2ETestBase {

  private static final ServerContainerBundle BUNDLE;
  private static final Path CONFIG_FILE;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static {
    BUNDLE = ServerContainerBundle.start();
    Runtime.getRuntime().addShutdownHook(new Thread(BUNDLE::close));

    try {
      CONFIG_FILE = Files.createTempFile("client-e2e-", ".properties");
      CONFIG_FILE.toFile().deleteOnExit();
      String config =
          "scalar.db.analytics.client.server.host="
              + BUNDLE.host()
              + "\n"
              + "scalar.db.analytics.client.server.catalog.port="
              + BUNDLE.port()
              + "\n";
      Files.write(CONFIG_FILE, config.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new RuntimeException("Failed to create client config file", e);
    }
  }

  private ByteArrayOutputStream stdout;
  private ByteArrayOutputStream stderr;
  private PrintStream originalStdout;
  private PrintStream originalStderr;

  @BeforeEach
  void setUpStreams() {
    originalStdout = System.out;
    originalStderr = System.err;
    stdout = new ByteArrayOutputStream();
    stderr = new ByteArrayOutputStream();
    try {
      System.setOut(new PrintStream(stdout, true, StandardCharsets.UTF_8.name()));
      System.setErr(new PrintStream(stderr, true, StandardCharsets.UTF_8.name()));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  @AfterEach
  void restoreStreams() {
    System.setOut(originalStdout);
    System.setErr(originalStderr);
  }

  /**
   * Executes CLI command with the given arguments. The {@code --config} flag is automatically
   * prepended.
   *
   * @param args the command arguments (e.g. "catalog", "create", "--catalog", "my_catalog")
   * @return the exit code
   */
  protected int execute(String... args) {
    String[] fullArgs = new String[args.length + 2];
    fullArgs[0] = "--config";
    fullArgs[1] = CONFIG_FILE.toAbsolutePath().toString();
    System.arraycopy(args, 0, fullArgs, 2, args.length);
    return new CommandLine(new CliRoot()).execute(fullArgs);
  }

  /** Returns captured stdout content. */
  protected String getStdout() {
    try {
      return stdout.toString(StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  /** Returns captured stderr content. */
  protected String getStderr() {
    try {
      return stderr.toString(StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  /** Parses stdout as a JSON tree. */
  protected JsonNode parseStdoutAsJson() {
    try {
      return OBJECT_MAPPER.readTree(getStdout());
    } catch (IOException e) {
      throw new RuntimeException("Failed to parse stdout as JSON: " + getStdout(), e);
    }
  }

  /** Returns the server container bundle for accessing container details. */
  protected static ServerContainerBundle bundle() {
    return BUNDLE;
  }

  /**
   * Creates a JDBC connection to the PostgreSQL container for test data setup.
   *
   * @return a new JDBC connection
   */
  protected static Connection createPostgresConnection() throws SQLException {
    return DriverManager.getConnection(
        BUNDLE.postgresContainer().getJdbcUrl(),
        BUNDLE.postgresContainer().getUsername(),
        BUNDLE.postgresContainer().getPassword());
  }

  /**
   * Executes a SQL statement against the PostgreSQL container.
   *
   * @param sql the SQL to execute
   */
  protected static void executeSql(String sql) throws SQLException {
    try (Connection conn = createPostgresConnection();
        Statement stmt = conn.createStatement()) {
      stmt.execute(sql);
    }
  }

  /**
   * Executes a CLI command without capturing output. Useful for setup/teardown in static
   * {@code @BeforeAll}/{@code @AfterAll} methods.
   *
   * @param args the command arguments
   * @return the exit code
   */
  protected static int executeStatic(String... args) {
    String[] fullArgs = new String[args.length + 2];
    fullArgs[0] = "--config";
    fullArgs[1] = CONFIG_FILE.toAbsolutePath().toString();
    System.arraycopy(args, 0, fullArgs, 2, args.length);
    return new CommandLine(new CliRoot()).execute(fullArgs);
  }

  /**
   * Builds a provider JSON string for registering a PostgreSQL data source. Uses the internal
   * Docker network hostname so the server container can connect to PostgreSQL.
   */
  protected static String buildPostgresProviderJson() {
    return String.format(
        "{\"type\":\"postgresql\",\"host\":\"%s\",\"port\":%d,\"username\":\"%s\",\"password\":\"%s\",\"database\":\"%s\"}",
        BUNDLE.postgresInternalHost(),
        BUNDLE.postgresInternalPort(),
        BUNDLE.postgresContainer().getUsername(),
        BUNDLE.postgresContainer().getPassword(),
        BUNDLE.postgresContainer().getDatabaseName());
  }
}
