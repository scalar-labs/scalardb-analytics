/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.scalar.db.analytics.client.exception.ClientException;
import com.scalar.db.analytics.client.exception.ErrorDetail.ConfigValidationError;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

/**
 * Tests how the CLI resolves the authentication credential from the environment and the
 * configuration file. The resolution itself is done by the SDK; these tests check that the CLI
 * gives the same results as the SDK and the Spark connector for the shared case table.
 */
class AuthConfigResolutionTest {
  private static final String USERNAME_ENV = "SCALAR_DB_ANALYTICS_CLIENT_AUTH_USERNAME";
  private static final String PASSWORD_ENV = "SCALAR_DB_ANALYTICS_CLIENT_AUTH_PASSWORD";
  private static final String USERNAME_KEY = "scalar.db.analytics.client.auth.username";
  private static final String PASSWORD_KEY = "scalar.db.analytics.client.auth.password";

  @TempDir Path tempDir;

  @TestFactory
  Stream<DynamicTest> sharedCases() throws IOException {
    return loadCases().stream().map(c -> DynamicTest.dynamicTest(c.name, () -> assertCase(c)));
  }

  @Test
  void givenBlankEnvUsername_shouldFallBackToFileUsername() throws Exception {
    Path file = writeConfig(USERNAME_KEY, "file_user", PASSWORD_KEY, "file_pass");

    AppConfig config = load(file, "   ", null);

    assertThat(config.auth()).isNotNull();
    assertThat(config.auth().username()).isEqualTo("file_user");
    assertThat(config.auth().password()).isEqualTo("file_pass");
  }

  @Test
  void givenPlaceholderReferringToAnotherKey_shouldKeepPasswordLiterally() throws Exception {
    String password = "p${scalar.db.analytics.client.server.host}q";
    Path file = writeConfig(USERNAME_KEY, "user", PASSWORD_KEY, password);

    AppConfig config = load(file, null, null);

    assertThat(config.auth()).isNotNull();
    assertThat(config.auth().password()).isEqualTo(password);
  }

  @Test
  void givenPlaceholderInEnvPassword_shouldKeepPasswordLiterally() throws Exception {
    Path file = writeConfig(USERNAME_KEY, "user");

    AppConfig config = load(file, null, "p${undefined}q");

    assertThat(config.auth()).isNotNull();
    assertThat(config.auth().password()).isEqualTo("p${undefined}q");
  }

  private void assertCase(Case c) throws Exception {
    Path file = writeConfig(c.fileUsername, c.filePassword, c.name);
    if (c.missingRelativeKey != null) {
      assertThatExceptionOfType(ClientException.class)
          .isThrownBy(() -> load(file, c.envUsername, c.envPassword))
          .extracting("detail")
          .isInstanceOfSatisfying(
              ConfigValidationError.class,
              error ->
                  assertThat(error.key())
                      .isEqualTo("scalar.db.analytics.client." + c.missingRelativeKey));
      return;
    }
    AppConfig config = load(file, c.envUsername, c.envPassword);
    if (c.expectedUsername == null) {
      assertThat(config.auth()).isNull();
    } else {
      assertThat(config.auth()).isNotNull();
      assertThat(config.auth().username()).isEqualTo(c.expectedUsername);
      assertThat(config.auth().password()).isEqualTo(c.expectedPassword);
    }
  }

  private static AppConfig load(
      Path file, @Nullable String envUsername, @Nullable String envPassword) throws Exception {
    return new EnvironmentVariables()
        .set(USERNAME_ENV, envUsername)
        .set(PASSWORD_ENV, envPassword)
        .set("SCALAR_DB_ANALYTICS_CLIENT_SERVER_HOST", null)
        .set("SCALAR_DB_ANALYTICS_CLIENT_SERVER_CATALOG_PORT", null)
        .execute(() -> AppConfig.load(file));
  }

  private Path writeConfig(String... keyValues) throws IOException {
    Properties props = new Properties();
    for (int i = 0; i < keyValues.length; i += 2) {
      props.setProperty(keyValues[i], keyValues[i + 1]);
    }
    return store(props, "client.properties");
  }

  private Path writeConfig(@Nullable String username, @Nullable String password, String name)
      throws IOException {
    Properties props = new Properties();
    if (username != null) {
      props.setProperty(USERNAME_KEY, username);
    }
    if (password != null) {
      props.setProperty(PASSWORD_KEY, password);
    }
    return store(props, name + ".properties");
  }

  private Path store(Properties props, String fileName) throws IOException {
    props.setProperty("scalar.db.analytics.client.server.host", "localhost");
    props.setProperty("scalar.db.analytics.client.server.catalog.port", "11051");
    Path file = tempDir.resolve(fileName);
    // Properties.store escapes leading spaces, so values reach the loader as they are.
    try (OutputStream out = Files.newOutputStream(file)) {
      props.store(out, null);
    }
    return file;
  }

  /** A row of the shared case table {@code credential-resolution-cases.tsv} in client-sdk. */
  private static final class Case {
    final String name;
    @Nullable final String envUsername;
    @Nullable final String envPassword;
    @Nullable final String fileUsername;
    @Nullable final String filePassword;
    @Nullable final String expectedUsername;
    @Nullable final String expectedPassword;
    @Nullable final String missingRelativeKey;

    Case(String[] cells) {
      name = cells[0];
      envUsername = parse(cells[1]);
      envPassword = parse(cells[2]);
      fileUsername = parse(cells[3]);
      filePassword = parse(cells[4]);
      expectedUsername = parse(cells[5]);
      expectedPassword = parse(cells[6]);
      missingRelativeKey = cells[7].equals("-") ? null : cells[7];
    }

    @Nullable
    private static String parse(String cell) {
      if (cell.equals("-")) {
        return null;
      }
      if (cell.length() < 2 || !cell.startsWith("\"") || !cell.endsWith("\"")) {
        throw new IllegalArgumentException("Invalid cell: " + cell);
      }
      return cell.substring(1, cell.length() - 1);
    }
  }

  private static List<Case> loadCases() throws IOException {
    InputStream in =
        Objects.requireNonNull(
            AuthConfigResolutionTest.class.getResourceAsStream("/credential-resolution-cases.tsv"));
    List<Case> cases = new ArrayList<>();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      boolean header = true;
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }
        if (header) {
          header = false;
          continue;
        }
        cases.add(new Case(line.split("\t", -1)));
      }
    }
    assertThat(cases).isNotEmpty();
    return cases;
  }
}
