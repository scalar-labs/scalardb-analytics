/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.scalar.db.analytics.api.auth.PasswordCredential;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.grpc.common.ChannelFactory;
import com.scalar.db.analytics.grpc.common.GrpcHealthCheckClient;
import com.scalar.db.analytics.grpc.common.HealthCheckClient;
import com.scalar.db.analytics.grpc.common.TlsConfig;
import com.scalar.db.analytics.sdk.auth.AuthClient;
import com.scalar.db.analytics.sdk.auth.BlockingGrpcInternalUserDirectoryClient;
import com.scalar.db.analytics.sdk.auth.BlockingGrpcUserClient;
import com.scalar.db.analytics.sdk.auth.ClientAuthInterceptor;
import com.scalar.db.analytics.sdk.auth.InternalUserDirectoryClient;
import com.scalar.db.analytics.sdk.auth.TokenManager;
import com.scalar.db.analytics.sdk.auth.UserClient;
import com.scalar.db.analytics.sdk.authz.BlockingGrpcPermissionClient;
import com.scalar.db.analytics.sdk.authz.BlockingGrpcRoleClient;
import com.scalar.db.analytics.sdk.authz.PermissionClient;
import com.scalar.db.analytics.sdk.authz.RoleClient;
import com.scalar.db.analytics.sdk.catalog.BlockingGrpcCatalogClient;
import com.scalar.db.analytics.sdk.catalog.CatalogClient;
import com.scalar.db.analytics.sdk.datasource.BlockingGrpcDataSourceClient;
import com.scalar.db.analytics.sdk.datasource.DataSourceClient;
import com.scalar.db.analytics.sdk.exception.GrpcExceptionMapper;
import com.scalar.db.analytics.sdk.namespace.BlockingGrpcNamespaceClient;
import com.scalar.db.analytics.sdk.namespace.NamespaceClient;
import com.scalar.db.analytics.sdk.retry.RetryConfig;
import com.scalar.db.analytics.sdk.table.BlockingGrpcTableClient;
import com.scalar.db.analytics.sdk.table.TableClient;
import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.jspecify.annotations.Nullable;

/**
 * Default implementation of ScalarDbAnalyticsClient using blocking (synchronous) gRPC stubs.
 *
 * <p>This implementation creates a single gRPC channel shared by all service clients. When password
 * authentication is configured, the channel is wrapped with an interceptor that automatically
 * attaches authentication headers.
 */
class BlockingGrpcAnalyticsClient implements ScalarDbAnalyticsClient {
  private static final Logger logger =
      Logger.getLogger(BlockingGrpcAnalyticsClient.class.getName());

  private final ManagedChannel managedChannel;
  @Nullable private final TokenManager tokenManager;
  private final CatalogClient catalogClient;
  private final DataSourceClient dataSourceClient;
  private final NamespaceClient namespaceClient;
  private final TableClient tableClient;
  private final InternalUserDirectoryClient internalUserDirectoryClient;
  private final UserClient userClient;
  private final RoleClient roleClient;
  private final PermissionClient permissionClient;
  private final HealthCheckClient healthCheckClient;

  @VisibleForTesting
  BlockingGrpcAnalyticsClient(ManagedChannel channel) {
    this(channel, channel, null);
  }

  BlockingGrpcAnalyticsClient(
      ManagedChannel managedChannel, Channel channel, @Nullable TokenManager tokenManager) {
    this.managedChannel = managedChannel;
    this.tokenManager = tokenManager;
    this.catalogClient = new BlockingGrpcCatalogClient(channel);
    this.dataSourceClient = new BlockingGrpcDataSourceClient(channel);
    this.namespaceClient = new BlockingGrpcNamespaceClient(channel);
    this.tableClient = new BlockingGrpcTableClient(channel);
    this.internalUserDirectoryClient = new BlockingGrpcInternalUserDirectoryClient(channel);
    this.userClient = new BlockingGrpcUserClient(channel);
    this.roleClient = new BlockingGrpcRoleClient(channel);
    this.permissionClient = new BlockingGrpcPermissionClient(channel);
    this.healthCheckClient = new GrpcHealthCheckClient(channel);
  }

  @Override
  public CatalogClient catalog() {
    return catalogClient;
  }

  @Override
  public DataSourceClient dataSource() {
    return dataSourceClient;
  }

  @Override
  public NamespaceClient namespace() {
    return namespaceClient;
  }

  @Override
  public TableClient table() {
    return tableClient;
  }

  @Override
  public InternalUserDirectoryClient internalUserDirectory() {
    return internalUserDirectoryClient;
  }

  @Override
  public UserClient user() {
    return userClient;
  }

  @Override
  public RoleClient role() {
    return roleClient;
  }

  @Override
  public PermissionClient permission() {
    return permissionClient;
  }

  @Override
  public Optional<HealthCheckClient> healthCheck() {
    return Optional.of(healthCheckClient);
  }

  @Override
  public void close() throws Exception {
    if (tokenManager != null) {
      tokenManager.close();
    }
    if (!managedChannel.isShutdown()) {
      managedChannel.shutdown();
      try {
        if (!managedChannel.awaitTermination(5, TimeUnit.SECONDS)) {
          managedChannel.shutdownNow();
        }
      } catch (InterruptedException e) {
        managedChannel.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  static Builder builder() {
    return new BuilderImpl();
  }

  static class BuilderImpl implements Builder {
    // Set through host() before the builder is used; build() rejects a host that was never set.
    @SuppressWarnings("NullAway.Init")
    private String host;

    private int port;
    @Nullable private TlsConfig tlsConfig;
    @Nullable private PasswordCredential credential;
    private RetryConfig retryConfig = RetryConfig.defaultConfig();
    private boolean tokenCacheEnabled;
    @Nullable private Path tokenCacheDir;

    @Override
    public Builder host(String host) {
      this.host = host;
      return this;
    }

    @Override
    public Builder port(int port) {
      this.port = port;
      return this;
    }

    @Override
    public Builder tlsConfig(@Nullable TlsConfig tlsConfig) {
      this.tlsConfig = tlsConfig;
      return this;
    }

    @Override
    public Builder passwordCredential(@Nullable PasswordCredential credential) {
      this.credential = credential;
      return this;
    }

    @Override
    public Builder retryConfig(@Nullable RetryConfig retryConfig) {
      this.retryConfig = retryConfig != null ? retryConfig : RetryConfig.defaultConfig();
      return this;
    }

    @Override
    public Builder enableTokenCache() {
      this.tokenCacheEnabled = true;
      return this;
    }

    @Override
    public Builder tokenCacheDir(Path tokenCacheDir) {
      this.tokenCacheEnabled = true;
      this.tokenCacheDir = tokenCacheDir;
      return this;
    }

    @Override
    public ScalarDbAnalyticsClient build() {
      if (host == null || host.trim().isEmpty()) {
        throw new AnalyticsException(
            AnalyticsErrorCode.INVALID_ARGUMENT, ImmutableMap.of("field", "host"));
      }
      if (port <= 0 || port > 65535) {
        throw new AnalyticsException(
            AnalyticsErrorCode.INVALID_ARGUMENT, ImmutableMap.of("field", "port"));
      }

      ManagedChannel managedChannel = createChannel();

      Channel channel = managedChannel;
      TokenManager tokenManager = null;

      if (credential != null) {
        if (tlsConfig == null) {
          logger.warning(
              "Authentication credentials are configured without TLS. "
                  + "Credentials will be transmitted in plaintext.");
        }
        try {
          AuthClient authClient = new AuthClient(channel);
          Path cacheDir = tokenCacheEnabled ? resolveTokenCacheDir() : null;
          tokenManager = new TokenManager(authClient, credential, cacheDir);
          tokenManager.initialize();
          ClientAuthInterceptor interceptor = new ClientAuthInterceptor(tokenManager);
          channel = ClientInterceptors.intercept(channel, interceptor);
        } catch (AnalyticsException e) {
          managedChannel.shutdownNow();
          throw e;
        } catch (Exception e) {
          managedChannel.shutdownNow();
          throw GrpcExceptionMapper.toAnalyticsException(e, "build");
        }
      }

      return new BlockingGrpcAnalyticsClient(managedChannel, channel, tokenManager);
    }

    private Path resolveTokenCacheDir() {
      if (tokenCacheDir != null) {
        return tokenCacheDir;
      }
      String xdgCacheHome = System.getenv("XDG_CACHE_HOME");
      Path cacheBase;
      if (xdgCacheHome != null && !xdgCacheHome.isEmpty()) {
        cacheBase = Paths.get(xdgCacheHome);
      } else {
        cacheBase = Paths.get(System.getProperty("user.home"), ".cache");
      }
      return cacheBase.resolve("scalardb-analytics").resolve("tokens");
    }

    @VisibleForTesting
    ManagedChannel createChannel() {
      ManagedChannelBuilder<?> builder;
      if (tlsConfig != null) {
        builder = ChannelFactory.createTlsBuilder(host, port, tlsConfig);
      } else {
        builder = ManagedChannelBuilder.forAddress(host, port).usePlaintext();
      }
      if (retryConfig.isEnabled()) {
        builder.enableRetry().defaultServiceConfig(retryConfig.toServiceConfig());
      } else {
        builder.disableRetry();
      }
      return builder.build();
    }
  }
}
