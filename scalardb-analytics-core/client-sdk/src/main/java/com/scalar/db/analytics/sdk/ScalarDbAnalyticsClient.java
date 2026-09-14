/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk;

import com.scalar.db.analytics.api.auth.PasswordCredential;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.grpc.common.HealthCheckClient;
import com.scalar.db.analytics.grpc.common.TlsConfig;
import com.scalar.db.analytics.sdk.auth.InternalUserDirectoryClient;
import com.scalar.db.analytics.sdk.auth.UserClient;
import com.scalar.db.analytics.sdk.authz.PermissionClient;
import com.scalar.db.analytics.sdk.authz.RoleClient;
import com.scalar.db.analytics.sdk.catalog.CatalogClient;
import com.scalar.db.analytics.sdk.datasource.DataSourceClient;
import com.scalar.db.analytics.sdk.namespace.NamespaceClient;
import com.scalar.db.analytics.sdk.retry.RetryConfig;
import com.scalar.db.analytics.sdk.table.TableClient;
import java.nio.file.Path;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Main client interface for ScalarDB Analytics.
 *
 * <p>This is the primary entry point for interacting with ScalarDB Analytics services. It provides
 * access to all service clients and manages the underlying gRPC channel lifecycle.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create a plaintext client
 * ScalarDbAnalyticsClient client = ScalarDbAnalyticsClient.builder()
 *     .host("localhost")
 *     .port(8080)
 *     .build();
 *
 * // Use catalog client
 * Catalog catalog = client.catalog().createCatalog("my-catalog");
 *
 * // Don't forget to close when done
 * client.close();
 * }</pre>
 *
 * <p>For TLS-enabled connections:
 *
 * <pre>{@code
 * TlsConfig tlsConfig = TlsConfig.builder()
 *     .caRootCertPath("/path/to/ca.crt")
 *     .overrideAuthority("server-hostname")
 *     .build();
 *
 * ScalarDbAnalyticsClient client = ScalarDbAnalyticsClient.builder()
 *     .host("server.example.com")
 *     .port(8443)
 *     .tlsConfig(tlsConfig)
 *     .build();
 * }</pre>
 */
public interface ScalarDbAnalyticsClient extends AutoCloseable {

  /**
   * Returns a client for catalog operations.
   *
   * @return the catalog client
   */
  CatalogClient catalog();

  /**
   * Returns a client for data source operations.
   *
   * @return the data source client
   */
  DataSourceClient dataSource();

  /**
   * Returns a client for namespace operations.
   *
   * @return the namespace client
   */
  NamespaceClient namespace();

  /**
   * Returns a client for table operations.
   *
   * @return the table client
   */
  TableClient table();

  /**
   * Returns a client for internal user directory operations.
   *
   * @return the internal user directory client
   */
  InternalUserDirectoryClient internalUserDirectory();

  /**
   * Returns a client for cross-backend user operations.
   *
   * @return the user client
   */
  UserClient user();

  /**
   * Returns a client for role management operations.
   *
   * @return the role client
   */
  RoleClient role();

  /**
   * Returns a client for permission management operations.
   *
   * @return the permission client
   */
  PermissionClient permission();

  /**
   * Returns a health check client if available.
   *
   * <p>The health check client allows checking the server's health status using the gRPC Health
   * Checking Protocol. This is an optional feature that may not be supported by all
   * implementations.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * client.healthCheck().ifPresent(health -> {
   *   try {
   *     if (health.check()) {
   *       System.out.println("Server is healthy");
   *     } else {
   *       System.out.println("Server is not healthy");
   *     }
   *   } catch (HealthCheckException e) {
   *     System.err.println("Health check failed: " + e.getMessage());
   *   }
   * });
   * }</pre>
   *
   * @return an Optional containing the health check client if supported, empty otherwise
   */
  Optional<HealthCheckClient> healthCheck();

  /**
   * Creates a new builder for constructing a ScalarDbAnalyticsClient.
   *
   * @return a new builder instance
   */
  static Builder builder() {
    return BlockingGrpcAnalyticsClient.builder();
  }

  /**
   * Builder for constructing ScalarDbAnalyticsClient instances.
   *
   * <p>Use this builder to configure the client's connection parameters and TLS settings.
   */
  interface Builder {

    /**
     * Sets the server host.
     *
     * @param host the server hostname or IP address
     * @return this builder
     */
    Builder host(String host);

    /**
     * Sets the server port.
     *
     * @param port the server port number
     * @return this builder
     */
    Builder port(int port);

    /**
     * Configures TLS settings for the connection.
     *
     * <p>If not set, the connection will use plaintext (insecure).
     *
     * @param tlsConfig the TLS configuration
     * @return this builder
     */
    Builder tlsConfig(@Nullable TlsConfig tlsConfig);

    /**
     * Configures password-based authentication.
     *
     * <p>When set, the client will authenticate with the server during {@link #build()} and
     * automatically attach authentication headers to all subsequent requests. The token is
     * automatically refreshed before expiry.
     *
     * <p>If not set, requests are sent without authentication headers.
     *
     * @param credential the username and password credential, or null to disable authentication
     * @return this builder
     */
    Builder passwordCredential(@Nullable PasswordCredential credential);

    /**
     * Enables disk-based token caching with the default directory.
     *
     * <p>When enabled alongside {@link #passwordCredential}, tokens are persisted to disk so that
     * short-lived processes can reuse valid tokens without re-authenticating. This is particularly
     * useful for CLI tools that run as one-shot processes.
     *
     * <p>The default directory is {@code $XDG_CACHE_HOME/scalardb-analytics/tokens/} (or {@code
     * ~/.cache/scalardb-analytics/tokens/} if {@code XDG_CACHE_HOME} is not set).
     *
     * <p>If not called, tokens are only kept in memory and are lost when the process exits.
     *
     * @return this builder
     */
    Builder enableTokenCache();

    /**
     * Configures retry behavior for transient gRPC errors.
     *
     * <p>By default, retry is enabled with exponential backoff. Use {@link RetryConfig#disabled()}
     * to disable retry, or {@link RetryConfig#builder()} to customize retry parameters.
     *
     * @param retryConfig the retry configuration, or null to use defaults
     * @return this builder
     */
    Builder retryConfig(@Nullable RetryConfig retryConfig);

    /**
     * Enables disk-based token caching with a custom directory.
     *
     * <p>This is equivalent to calling {@link #enableTokenCache()} but overrides the default
     * directory with the specified path.
     *
     * @param tokenCacheDir the directory path for token cache files
     * @return this builder
     */
    Builder tokenCacheDir(Path tokenCacheDir);

    /**
     * Builds the ScalarDbAnalyticsClient instance.
     *
     * @return a new ScalarDbAnalyticsClient instance
     * @throws AnalyticsException if the client cannot be created
     */
    ScalarDbAnalyticsClient build();
  }
}
