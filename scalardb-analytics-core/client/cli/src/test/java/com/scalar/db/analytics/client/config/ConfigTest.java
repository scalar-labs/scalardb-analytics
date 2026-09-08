/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.scalar.db.analytics.client.exception.ClientException;
import com.scalar.db.analytics.client.exception.ErrorDetail.ConfigValidationError;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
class ConfigTest {
  @Nested
  class Load {
    @Nested
    class ConfigFromFile {
      Path configFile;

      @BeforeEach
      void setUp(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve("scalardb-analytics");
        Files.createDirectories(configDir);
        configFile = configDir.resolve("client.properties");
        Files.createFile(configFile);
      }

      @Test
      void givenValidProperties_shouldLoad() throws IOException {
        // Arrange
        String properties =
            "scalar.db.analytics.client.server.host=localhost\n"
                + "scalar.db.analytics.client.server.catalog.port=1234\n";
        Files.write(configFile, properties.getBytes(StandardCharsets.UTF_8));

        // Act
        AppConfig config = AppConfig.load(configFile);

        // Assert
        assertThat(config).isNotNull();
        assertThat(config.catalogServer().host()).isEqualTo("localhost");
        assertThat(config.catalogServer().port()).isEqualTo(1234);
        assertThat(config.catalogServer().tlsEnabled()).isFalse();
        assertThat(config.catalogServer().caRootCertPath()).isNull();
      }

      @Test
      void givenValidPropertiesWithTls_shouldLoad() throws IOException {
        // Arrange
        String properties =
            "scalar.db.analytics.client.server.host=localhost\n"
                + "scalar.db.analytics.client.server.catalog.port=1234\n"
                + "scalar.db.analytics.client.server.tls.enabled=true\n"
                + "scalar.db.analytics.client.server.tls.ca_root_cert_path=/path/to/ca-cert.pem\n";
        Files.write(configFile, properties.getBytes(StandardCharsets.UTF_8));

        // Act
        AppConfig config = AppConfig.load(configFile);

        // Assert
        assertThat(config).isNotNull();
        assertThat(config.catalogServer().host()).isEqualTo("localhost");
        assertThat(config.catalogServer().port()).isEqualTo(1234);
        assertThat(config.catalogServer().tlsEnabled()).isTrue();
        assertThat(config.catalogServer().caRootCertPath()).isEqualTo("/path/to/ca-cert.pem");
      }

      @Test
      void givenPropertiesMissingHost_shouldThrow() throws IOException {
        // Arrange
        String properties = "scalar.db.analytics.client.server.catalog.port=1234\n";
        Files.write(configFile, properties.getBytes(StandardCharsets.UTF_8));

        // Act & Assert
        assertThatExceptionOfType(ClientException.class)
            .isThrownBy(() -> AppConfig.load(configFile))
            .extracting("detail")
            .isInstanceOfSatisfying(
                ConfigValidationError.class,
                error ->
                    assertThat(error.key()).isEqualTo("scalar.db.analytics.client.server.host"));
      }

      @Test
      void givenTlsEnabledButMissingCaCert_shouldUseSystemDefault() throws IOException {
        // Arrange
        String properties =
            "scalar.db.analytics.client.server.host=localhost\n"
                + "scalar.db.analytics.client.server.catalog.port=1234\n"
                + "scalar.db.analytics.client.server.tls.enabled=true\n";
        Files.write(configFile, properties.getBytes(StandardCharsets.UTF_8));

        // Act
        AppConfig config = AppConfig.load(configFile);

        // Assert
        assertThat(config).isNotNull();
        assertThat(config.catalogServer().host()).isEqualTo("localhost");
        assertThat(config.catalogServer().port()).isEqualTo(1234);
        assertThat(config.catalogServer().tlsEnabled()).isTrue();
        assertThat(config.catalogServer().caRootCertPath()).isNull();
      }

      @Test
      void givenTlsEnabledWithEmptyCaCert_shouldUseSystemDefault() throws IOException {
        // Arrange
        String properties =
            "scalar.db.analytics.client.server.host=localhost\n"
                + "scalar.db.analytics.client.server.catalog.port=1234\n"
                + "scalar.db.analytics.client.server.tls.enabled=true\n"
                + "scalar.db.analytics.client.server.tls.ca_cert_path=\n";
        Files.write(configFile, properties.getBytes(StandardCharsets.UTF_8));

        // Act
        AppConfig config = AppConfig.load(configFile);

        // Assert
        assertThat(config).isNotNull();
        assertThat(config.catalogServer().host()).isEqualTo("localhost");
        assertThat(config.catalogServer().port()).isEqualTo(1234);
        assertThat(config.catalogServer().tlsEnabled()).isTrue();
        assertThat(config.catalogServer().caRootCertPath()).isNull();
      }

      @Test
      void givenPropertiesMissingPort_shouldThrow() throws IOException {
        // Arrange
        String properties = "scalar.db.analytics.client.server.host=localhost\n";
        Files.write(configFile, properties.getBytes(StandardCharsets.UTF_8));

        // Act & Assert
        assertThatExceptionOfType(ClientException.class)
            .isThrownBy(() -> AppConfig.load(configFile))
            .extracting("detail")
            .isInstanceOfSatisfying(
                ConfigValidationError.class,
                error ->
                    assertThat(error.key())
                        .isEqualTo("scalar.db.analytics.client.server.catalog.port"));
      }

      @Test
      void givenConfigPathFromEnv_shouldLoad() throws Exception {
        Path configPathFromEnv =
            Paths.get(
                Objects.requireNonNull(getClass().getClassLoader().getResource("client.properties"))
                    .toURI());
        new EnvironmentVariables()
            .set("SCALAR_DB_ANALYTICS_CONFIG_PATH", configPathFromEnv.toAbsolutePath().toString())
            .execute(
                () -> {
                  AppConfig config = AppConfig.load();
                  assertThat(config).isNotNull();
                  assertThat(config.catalogServer().host()).isEqualTo("localhost");
                  assertThat(config.catalogServer().port()).isEqualTo(3456);
                  assertThat(config.catalogServer().tlsEnabled()).isFalse();
                  assertThat(config.catalogServer().caRootCertPath()).isNull();
                });
      }
    }

    @Nested
    class AuthConfig {
      Path configFile;

      @BeforeEach
      void setUp(@TempDir Path tempDir) throws IOException {
        Path configDir = tempDir.resolve("scalardb-analytics");
        Files.createDirectories(configDir);
        configFile = configDir.resolve("client.properties");
        Files.createFile(configFile);
      }

      @Test
      void givenBothUsernameAndPassword_shouldLoadAuthConfig() throws IOException {
        // Arrange
        String properties =
            "scalar.db.analytics.client.server.host=localhost\n"
                + "scalar.db.analytics.client.server.catalog.port=1234\n"
                + "scalar.db.analytics.client.auth.username=user\n"
                + "scalar.db.analytics.client.auth.password=pass\n";
        Files.write(configFile, properties.getBytes(StandardCharsets.UTF_8));

        // Act
        AppConfig config = AppConfig.load(configFile);

        // Assert
        assertThat(config.auth()).isNotNull();
        assertThat(config.auth().username()).isEqualTo("user");
        assertThat(config.auth().password()).isEqualTo("pass");
      }

      @Test
      void givenNoAuthConfig_shouldReturnNull() throws IOException {
        // Arrange
        String properties =
            "scalar.db.analytics.client.server.host=localhost\n"
                + "scalar.db.analytics.client.server.catalog.port=1234\n";
        Files.write(configFile, properties.getBytes(StandardCharsets.UTF_8));

        // Act
        AppConfig config = AppConfig.load(configFile);

        // Assert
        assertThat(config.auth()).isNull();
      }

      @Test
      void givenUsernameOnly_shouldThrow() throws IOException {
        // Arrange
        String properties =
            "scalar.db.analytics.client.server.host=localhost\n"
                + "scalar.db.analytics.client.server.catalog.port=1234\n"
                + "scalar.db.analytics.client.auth.username=user\n";
        Files.write(configFile, properties.getBytes(StandardCharsets.UTF_8));

        // Act & Assert
        assertThatExceptionOfType(ClientException.class)
            .isThrownBy(() -> AppConfig.load(configFile))
            .extracting("detail")
            .isInstanceOfSatisfying(
                ConfigValidationError.class,
                error ->
                    assertThat(error.key()).isEqualTo("scalar.db.analytics.client.auth.password"));
      }

      @Test
      void givenEmptyUsername_shouldReturnNull() throws IOException {
        // Arrange
        String properties =
            "scalar.db.analytics.client.server.host=localhost\n"
                + "scalar.db.analytics.client.server.catalog.port=1234\n"
                + "scalar.db.analytics.client.auth.username=\n";
        Files.write(configFile, properties.getBytes(StandardCharsets.UTF_8));

        // Act
        AppConfig config = AppConfig.load(configFile);

        // Assert - empty username is normalized to null, so auth config is null
        assertThat(config.auth()).isNull();
      }

      @Test
      void givenWhitespaceUsername_shouldReturnNull() throws IOException {
        // Arrange
        String properties =
            "scalar.db.analytics.client.server.host=localhost\n"
                + "scalar.db.analytics.client.server.catalog.port=1234\n"
                + "scalar.db.analytics.client.auth.username=   \n";
        Files.write(configFile, properties.getBytes(StandardCharsets.UTF_8));

        // Act
        AppConfig config = AppConfig.load(configFile);

        // Assert - whitespace username is normalized to null, so auth config is null
        assertThat(config.auth()).isNull();
      }

      @Test
      void givenWhitespaceUsernameWithPassword_shouldThrow() throws IOException {
        // Arrange
        String properties =
            "scalar.db.analytics.client.server.host=localhost\n"
                + "scalar.db.analytics.client.server.catalog.port=1234\n"
                + "scalar.db.analytics.client.auth.username=   \n"
                + "scalar.db.analytics.client.auth.password=pass\n";
        Files.write(configFile, properties.getBytes(StandardCharsets.UTF_8));

        // Act & Assert - blank username normalized to null, password-only triggers error
        assertThatExceptionOfType(ClientException.class)
            .isThrownBy(() -> AppConfig.load(configFile))
            .extracting("detail")
            .isInstanceOfSatisfying(
                ConfigValidationError.class,
                error ->
                    assertThat(error.key()).isEqualTo("scalar.db.analytics.client.auth.username"));
      }

      @Test
      void givenPasswordOnly_shouldThrow() throws IOException {
        // Arrange
        String properties =
            "scalar.db.analytics.client.server.host=localhost\n"
                + "scalar.db.analytics.client.server.catalog.port=1234\n"
                + "scalar.db.analytics.client.auth.password=pass\n";
        Files.write(configFile, properties.getBytes(StandardCharsets.UTF_8));

        // Act & Assert
        assertThatExceptionOfType(ClientException.class)
            .isThrownBy(() -> AppConfig.load(configFile))
            .extracting("detail")
            .isInstanceOfSatisfying(
                ConfigValidationError.class,
                error ->
                    assertThat(error.key()).isEqualTo("scalar.db.analytics.client.auth.username"));
      }
    }

    @Nested
    class ConfigFromEnv {
      @Test
      void shouldLoadFromEnvironmentVariable() throws Exception {
        new EnvironmentVariables()
            .set("SCALAR_DB_ANALYTICS_CLIENT_SERVER_HOST", "host_from_env")
            .set("SCALAR_DB_ANALYTICS_CLIENT_SERVER_CATALOG_PORT", "2345")
            .execute(
                () -> {
                  AppConfig config = AppConfig.load();
                  assertThat(config).isNotNull();
                  assertThat(config.catalogServer().host()).isEqualTo("host_from_env");
                  assertThat(config.catalogServer().port()).isEqualTo(2345);
                  assertThat(config.catalogServer().tlsEnabled()).isFalse();
                  assertThat(config.catalogServer().caRootCertPath()).isNull();
                });
      }

      @Test
      void shouldLoadTlsConfigFromEnvironmentVariable() throws Exception {
        new EnvironmentVariables()
            .set("SCALAR_DB_ANALYTICS_CLIENT_SERVER_HOST", "host_from_env")
            .set("SCALAR_DB_ANALYTICS_CLIENT_SERVER_CATALOG_PORT", "2345")
            .set("SCALAR_DB_ANALYTICS_CLIENT_SERVER_TLS_ENABLED", "true")
            .set("SCALAR_DB_ANALYTICS_CLIENT_SERVER_TLS_CA_ROOT_CERT_PATH", "/env/ca-cert.pem")
            .execute(
                () -> {
                  AppConfig config = AppConfig.load();
                  assertThat(config).isNotNull();
                  assertThat(config.catalogServer().host()).isEqualTo("host_from_env");
                  assertThat(config.catalogServer().port()).isEqualTo(2345);
                  assertThat(config.catalogServer().tlsEnabled()).isTrue();
                  assertThat(config.catalogServer().caRootCertPath()).isEqualTo("/env/ca-cert.pem");
                });
      }

      @Test
      void shouldLoadAuthConfigFromEnvironmentVariable() throws Exception {
        new EnvironmentVariables()
            .set("SCALAR_DB_ANALYTICS_CLIENT_SERVER_HOST", "localhost")
            .set("SCALAR_DB_ANALYTICS_CLIENT_SERVER_CATALOG_PORT", "1234")
            .set("SCALAR_DB_ANALYTICS_CLIENT_AUTH_USERNAME", "user_from_env")
            .set("SCALAR_DB_ANALYTICS_CLIENT_AUTH_PASSWORD", "pass_from_env")
            .execute(
                () -> {
                  AppConfig config = AppConfig.load();
                  assertThat(config).isNotNull();
                  assertThat(config.auth()).isNotNull();
                  assertThat(config.auth().username()).isEqualTo("user_from_env");
                  assertThat(config.auth().password()).isEqualTo("pass_from_env");
                });
      }

      @Test
      void givenEnvWithConfigPath_envShouldHavePrecedence(@TempDir Path tempDir) throws Exception {
        Path configDir = tempDir.resolve("scalardb-analytics");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("client.properties");
        String properties =
            "scalar.db.analytics.client.server.host=localhost\n"
                + "scalar.db.analytics.client.server.catalog.port=1234\n";
        Files.write(configFile, properties.getBytes(StandardCharsets.UTF_8));

        new EnvironmentVariables()
            .set("SCALAR_DB_ANALYTICS_CLIENT_SERVER_HOST", "host_from_env")
            .execute(
                () -> {
                  AppConfig config = AppConfig.load(configFile);
                  assertThat(config).isNotNull();
                  assertThat(config.catalogServer().host()).isEqualTo("host_from_env");
                  assertThat(config.catalogServer().port()).isEqualTo(1234);
                });
      }
    }
  }
}
