/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.config;

import com.scalar.db.analytics.client.exception.ClientException;
import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

/** Represents the application configuration for the Scalar DB Analytics client. */
public class AppConfig {
  private final CatalogServerConfig catalogServer;
  @Nullable private final AuthConfig auth;

  public AppConfig(CatalogServerConfig catalogServer, @Nullable AuthConfig auth) {
    this.catalogServer = catalogServer;
    this.auth = auth;
  }

  public CatalogServerConfig catalogServer() {
    return catalogServer;
  }

  @Nullable
  public AuthConfig auth() {
    return auth;
  }

  /**
   * Configuration for connecting to the ScalarDB Analytics catalog server. Contains the connection
   * details required to establish communication with the server.
   */
  public static class CatalogServerConfig {
    private final String host;
    private final int port;
    private final boolean tlsEnabled;
    @Nullable private final String caRootCertPath;
    @Nullable private final String tlsOverrideAuthority;

    public CatalogServerConfig(
        String host,
        int port,
        boolean tlsEnabled,
        @Nullable String caRootCertPath,
        @Nullable String tlsOverrideAuthority) {
      this.host = host;
      this.port = port;
      this.tlsEnabled = tlsEnabled;
      this.caRootCertPath = caRootCertPath;
      this.tlsOverrideAuthority = tlsOverrideAuthority;
    }

    public String host() {
      return host;
    }

    public int port() {
      return port;
    }

    public boolean tlsEnabled() {
      return tlsEnabled;
    }

    @Nullable
    public String caRootCertPath() {
      return caRootCertPath;
    }

    @Nullable
    public String tlsOverrideAuthority() {
      return tlsOverrideAuthority;
    }
  }

  /** Authentication configuration for connecting to the server. */
  public static class AuthConfig {
    private final String username;
    private final String password;

    public AuthConfig(String username, String password) {
      this.username = username;
      this.password = password;
    }

    public String username() {
      return username;
    }

    public String password() {
      return password;
    }
  }

  /**
   * Loads the application configuration from the default path, which is determined by the
   * environment variable {@code SCALAR_DB_ANALYTICS_CONFIG_PATH} or the default XDG config
   * directory.
   *
   * @return the loaded AppConfig
   * @throws ClientException if there is an error loading or validating the configuration
   */
  public static AppConfig load() {
    return ConfigLoader.load();
  }

  /**
   * Loads the application configuration from the specified path.
   *
   * @param configPath the path to the configuration file
   * @return the loaded AppConfig
   * @throws ClientException if there is an error loading or validating the configuration
   */
  public static AppConfig load(Path configPath) {
    return ConfigLoader.load(configPath);
  }
}
