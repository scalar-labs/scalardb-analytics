/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.config;

import org.aeonbits.owner.Config;
import org.jspecify.annotations.Nullable;

public interface UnvalidatedConfig extends Config {
  final String CATALOG_SERVER_HOST_KEY = "scalar.db.analytics.client.server.host";
  final String CATALOG_SERVER_PORT_KEY = "scalar.db.analytics.client.server.catalog.port";
  final String CATALOG_SERVER_TLS_ENABLED_KEY = "scalar.db.analytics.client.server.tls.enabled";
  final String CATALOG_SERVER_TLS_CA_ROOT_CERT_PATH_KEY =
      "scalar.db.analytics.client.server.tls.ca_root_cert_path";
  final String CATALOG_SERVER_TLS_OVERRIDE_AUTHORITY_KEY =
      "scalar.db.analytics.client.server.tls.override_authority";

  @Nullable
  @Config.Key(CATALOG_SERVER_HOST_KEY)
  @EnvVar
  String host();

  @Nullable
  @Config.Key(CATALOG_SERVER_PORT_KEY)
  @EnvVar
  Integer port();

  @Config.Key(CATALOG_SERVER_TLS_ENABLED_KEY)
  @Config.DefaultValue("false")
  @EnvVar
  Boolean tlsEnabled();

  @Nullable
  @Config.Key(CATALOG_SERVER_TLS_CA_ROOT_CERT_PATH_KEY)
  @EnvVar("SCALAR_DB_ANALYTICS_CLIENT_SERVER_TLS_CA_ROOT_CERT_PATH")
  String caRootCertPath();

  @Nullable
  @Config.Key(CATALOG_SERVER_TLS_OVERRIDE_AUTHORITY_KEY)
  @EnvVar
  String tlsOverrideAuthority();

  final String DEFAULT_CATALOG_KEY = "scalar.db.analytics.client.default.catalog";

  @Nullable
  @Config.Key(DEFAULT_CATALOG_KEY)
  @EnvVar
  String defaultCatalog();

  // The authentication keys (scalar.db.analytics.client.auth.*) are not declared here. They are
  // owned by the SDK's ClientConfigKey and resolved by ClientConfigResolver; see ConfigLoader.
}
