/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.config;

import com.scalar.db.analytics.client.exception.ClientException;
import com.scalar.db.analytics.client.exception.ConfigValidationErrorDetail;
import com.scalar.db.analytics.client.exception.ErrorDetail.ConfigLoadError;
import com.scalar.db.analytics.client.exception.ErrorDetail.ConfigValidationError;
import com.scalar.db.analytics.sdk.config.ClientConfigException;
import com.scalar.db.analytics.sdk.config.ClientConfigResolver;
import com.scalar.db.analytics.sdk.config.ConfigSource;
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
 * scalar.db.analytics.client.default.catalog=my_catalog
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
 * SCALAR_DB_ANALYTICS_CLIENT_DEFAULT_CATALOG=my_catalog
 * SCALAR_DB_ANALYTICS_CLIENT_AUTH_USERNAME=alice
 * SCALAR_DB_ANALYTICS_CLIENT_AUTH_PASSWORD=secret
 * }</pre>
 *
 * <h3>Precedence</h3>
 *
 * <p>When both configuration sources are present, <b>environment variables take precedence</b> over
 * configuration file values. This allows for easy override of file-based configuration in different
 * deployment environments without modifying the configuration file.
 *
 * <p>The authentication credential ({@code scalar.db.analytics.client.auth.*}) is resolved by the
 * SDK's {@link ClientConfigResolver}, so it follows the same rules as the SDK and the Spark
 * connector: a blank username in the environment falls through to the file, and the password is
 * used as is.
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
    Properties fileProps = loadFile(configPath);
    return validateConfig(createUnvalidated(fileProps), fileProps);
  }

  /**
   * Loads only the default catalog from the specified path and the environment, without validating
   * the rest of the configuration.
   *
   * @param configPath the path to the configuration file
   * @return the default catalog, or {@code null} if none is set
   * @throws ClientException if there is an error loading the configuration
   */
  @Nullable
  static String loadDefaultCatalog(Path configPath) {
    return normalizeBlank(createUnvalidated(loadFile(configPath)).defaultCatalog());
  }

  private static Properties loadFile(Path configPath) {
    try {
      return loadPropertiesFromFile(configPath);
    } catch (IOException e) {
      throw new ClientException(new ConfigLoadError(e));
    }
  }

  private static UnvalidatedConfig createUnvalidated(Properties fileProps) {
    return ConfigFactory.create(
        UnvalidatedConfig.class, loadPropertiesFromEnvironment(), fileProps);
  }

  private static AppConfig validateConfig(UnvalidatedConfig config, Properties fileProps) {
    return new AppConfig(
        validateCatalogServerConfig(config),
        validateAuthConfig(fileProps),
        // Blank is treated as unset, so that "default.catalog=" disables the default.
        normalizeBlank(config.defaultCatalog()));
  }

  private static AppConfig.@Nullable AuthConfig validateAuthConfig(Properties fileProps) {
    // The raw file properties are used rather than the Owner config, so that Owner's variable
    // expansion does not alter the password.
    try {
      return ClientConfigResolver.resolve(
              ConfigSource.environment(), ConfigSource.properties(fileProps))
          .passwordCredential()
          .map(c -> new AppConfig.AuthConfig(c.getUsername(), c.getPassword()))
          .orElse(null);
    } catch (ClientConfigException e) {
      throw new ClientException(
          new ConfigValidationError(
              e.key().propertyKey(), ConfigValidationErrorDetail.REQUIRED_KEY_NOT_SET));
    }
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
