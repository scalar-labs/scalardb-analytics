/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.module;

import com.scalar.db.analytics.api.auth.PasswordCredential;
import com.scalar.db.analytics.client.config.AppConfig;
import com.scalar.db.analytics.grpc.common.TlsConfig;
import com.scalar.db.analytics.sdk.ScalarDbAnalyticsClient;

public class Modules implements AutoCloseable {
  private final ScalarDbAnalyticsClient client;

  public Modules(ScalarDbAnalyticsClient client) {
    this.client = client;
  }

  public ScalarDbAnalyticsClient client() {
    return client;
  }

  public static Modules initialize(AppConfig config) {
    AppConfig.CatalogServerConfig serverConfig = config.catalogServer();

    ScalarDbAnalyticsClient.Builder builder =
        ScalarDbAnalyticsClient.builder().host(serverConfig.host()).port(serverConfig.port());

    if (serverConfig.tlsEnabled()) {
      TlsConfig.TlsConfigBuilder tlsBuilder = TlsConfig.builder();

      if (serverConfig.caRootCertPath() != null) {
        tlsBuilder.caRootCertPath(serverConfig.caRootCertPath());
      }

      if (serverConfig.tlsOverrideAuthority() != null) {
        tlsBuilder.overrideAuthority(serverConfig.tlsOverrideAuthority());
      }

      builder.tlsConfig(tlsBuilder.build());
    }

    AppConfig.AuthConfig authConfig = config.auth();
    if (authConfig != null) {
      builder.passwordCredential(
          new PasswordCredential(authConfig.username(), authConfig.password()));
      builder.enableTokenCache();
    }

    ScalarDbAnalyticsClient client = builder.build();
    return new Modules(client);
  }

  @Override
  public void close() {
    try {
      client.close();
    } catch (Exception e) {
      throw new RuntimeException("Failed to close client", e);
    }
  }
}
