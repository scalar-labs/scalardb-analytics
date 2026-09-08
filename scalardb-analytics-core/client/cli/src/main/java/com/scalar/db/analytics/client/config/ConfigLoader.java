/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.config;

import com.scalar.db.analytics.client.exception.ClientException;
import com.scalar.db.analytics.client.exception.ConfigValidationErrorDetail;
import com.scalar.db.analytics.client.exception.ErrorDetail.ConfigLoadError;
import com.scalar.db.analytics.client.exception.ErrorDetail.ConfigValidationError;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.aeonbits.owner.ConfigFactory;
import org.jspecify.annotations.Nullable;

/**
 * Loads and validates configuration for the ScalarDB Analytics client.
 *
 * <p>Configuration can be provided through two sources:
 *
 * <ol>
 *   <li><b>Configuration file</b>: A properties file containing key-value pairs
 *   <li><b>Environment variables</b>: System environment variables with specific naming convention
 * </ol>
 *
 * <h3>Configuration File</h3>
 *
 * <p>The configuration file should be in the standard Java properties format. Example:
 *
 * <pre>{@code
 * scalar.db.analytics.client.server.host=localhost
 * scalar.db.analytics.client.server.catalog.port=8080
 * scalar.db.analytics.client.server.tls.enabled=true
 * scalar.db.analytics.client.server.tls.ca_root_cert_path=/path/to/ca-cert.pem
 * scalar.db.analytics.client.server.tls.override_authority=example.com
 * }</pre>
 *
 * <h3>Environment Variables</h3>
 *
 * <p>Environment variables must follow a specific naming convention where dots are replaced with
 * underscores and the entire name is uppercase. Example:
 *
 * <pre>{@code
 * SCALAR_DB_ANALYTICS_CLIENT_SERVER_HOST=localhost
 * SCALAR_DB_ANALYTICS_CLIENT_SERVER_CATALOG_PORT=8080
 * SCALAR_DB_ANALYTICS_CLIENT_SERVER_TLS_ENABLED=true
 * SCALAR_DB_ANALYTICS_CLIENT_SERVER_TLS_CA_ROOT_CERT_PATH=/path/to/ca-cert.pem
 * SCALAR_DB_ANALYTICS_CLIENT_SERVER_TLS_OVERRIDE_AUTHORITY=example.com
 * }</pre>
 *
 * <h3>Precedence</h3>
 *
 * <p>When both configuration sources are present, <b>environment variables take precedence</b> over
 * configuration file values. This allows for easy override of file-based configuration in different
 * deployment environments without modifying the configuration file.
 *
 * <h3>Configuration File Location</h3>
 *
 * <p>The configuration file location can be specified through:
 *
 * <ol>
 *   <li>The {@code SCALAR_DB_ANALYTICS_CONFIG_PATH} environment variable
 *   <li>The default location: {@code ${XDG_CONFIG_HOME}/scalardb-analytics/client.properties}
 * </ol>
 *
 * @see AppConfig
 * @see UnvalidatedConfig
 */
class ConfigLoader {
  private static final String CONFIG_DIR = "scalardb-analytics";
  private static final String CONFIG_FILE = "client.properties";
  public static final String CONFIG_FILE_PATH_ENV_VAR = "SCALAR_DB_ANALYTICS_CONFIG_PATH";
  public static final String XDG_CONFIG_HOME = "XDG_CONFIG_HOME";

  /**
   * Loads the configuration from the default path, which is determined by the environment variable
   * {@code SCALAR_DB_ANALYTICS_CONFIG_PATH} or the default XDG config directory.
   *
   * @return the loaded AppConfig
   * @throws ClientException if there is an error loading or validating the configuration
   */
  static AppConfig load() {
    return load(getConfigFilePath());
  }

  /**
   * Loads the configuration from the specified path.
   *
   * @param configPath the path to the configuration file
   * @return the loaded AppConfig
   * @throws ClientException if there is an error loading or validating the configuration
   */
  static AppConfig load(Path configPath) {
    try {
      UnvalidatedConfig unvalidatedConfig =
          ConfigFactory.create(
              UnvalidatedConfig.class,
              loadPropertiesFromEnvironment(),
              loadPropertiesFromFile(configPath));

      return validateConfig(unvalidatedConfig);
    } catch (IOException e) {
      throw new ClientException(new ConfigLoadError(e));
    }
  }

  private static AppConfig validateConfig(UnvalidatedConfig config) {
    return new AppConfig(validateCatalogServerConfig(config), validateAuthConfig(config));
  }

  private static AppConfig.@Nullable AuthConfig validateAuthConfig(UnvalidatedConfig config) {
    // Normalize blank username to null so that "auth.username=" is treated as unset.
    // Password is intentionally not checked: empty passwords may be valid in some configurations.
    String username = normalizeBlank(config.authUsername());
    String password = config.authPassword();

    if (username == null) {
      if (password == null) {
        return null;
      }
      throw new ClientException(
          new ConfigValidationError(
              UnvalidatedConfig.AUTH_USERNAME_KEY,
              ConfigValidationErrorDetail.REQUIRED_KEY_NOT_SET));
    }
    if (password == null) {
      throw new ClientException(
          new ConfigValidationError(
              UnvalidatedConfig.AUTH_PASSWORD_KEY,
              ConfigValidationErrorDetail.REQUIRED_KEY_NOT_SET));
    }
    return new AppConfig.AuthConfig(username, password);
  }

  private static AppConfig.CatalogServerConfig validateCatalogServerConfig(
      UnvalidatedConfig config) {
    String host = config.host();
    if (host == null) {
      throw new ClientException(
          new ConfigValidationError(
              UnvalidatedConfig.CATALOG_SERVER_HOST_KEY,
              ConfigValidationErrorDetail.REQUIRED_KEY_NOT_SET));
    }

    Integer port = config.port();
    if (port == null) {
      throw new ClientException(
          new ConfigValidationError(
              UnvalidatedConfig.CATALOG_SERVER_PORT_KEY,
              ConfigValidationErrorDetail.REQUIRED_KEY_NOT_SET));
    }

    boolean tlsEnabled = config.tlsEnabled();
    String caRootCertPath = config.caRootCertPath();
    if (tlsEnabled && (caRootCertPath == null || caRootCertPath.trim().isEmpty())) {
      caRootCertPath = null;
    }
    String tlsOverrideAuthority = config.tlsOverrideAuthority();
    if (tlsOverrideAuthority != null && tlsOverrideAuthority.trim().isEmpty()) {
      tlsOverrideAuthority = null;
    }

    return new AppConfig.CatalogServerConfig(
        host, port, tlsEnabled, caRootCertPath, tlsOverrideAuthority);
  }

  private static Properties loadPropertiesFromFile(Path configPath) throws IOException {
    Properties fileProps = new Properties();
    if (Files.exists(configPath)) {
      try (FileInputStream fis = new FileInputStream(configPath.toFile())) {
        fileProps.load(fis);
      }
    }
    return fileProps;
  }

  private static Properties loadPropertiesFromEnvironment() {
    return ConfigMappingUtils.loadPropertiesFromEnvironment(UnvalidatedConfig.class);
  }

  /**
   * Returns the path to the configuration file. It first checks the environment variable {@code
   * SCALAR_DB_ANALYTICS_CONFIG_PATH}. If it is not set, it returns the default path in the XDG
   * config directory, which is `${XDG_CONFIG_HOME}/scalardb-analytics/client.properties`.
   */
  static Path getConfigFilePath() {
    String envConfigPath = System.getenv(CONFIG_FILE_PATH_ENV_VAR);
    if (envConfigPath != null) {
      return Paths.get(envConfigPath);
    } else {
      return getDefaultConfigFilePath();
    }
  }

  static Path getDefaultConfigFilePath() {
    return getXDGConfigDir().resolve(CONFIG_DIR).resolve(CONFIG_FILE);
  }

  @Nullable
  private static String normalizeBlank(@Nullable String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static Path getXDGConfigDir() {
    String xdgConfigHome = System.getenv(XDG_CONFIG_HOME);
    if (xdgConfigHome != null) {
      return Paths.get(xdgConfigHome);
    } else {
      return Paths.get(System.getProperty("user.home"), ".config");
    }
  }
}
