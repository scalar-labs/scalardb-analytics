/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ServerConfigurationTest {

  @Test
  void givenValidCatalogConfig_shouldCreateConfig() {
    // Create catalog config
    CatalogServerProperties config = new CatalogServerProperties(11052);

    assertThat(config.getPort()).isEqualTo(11052);
  }

  @Test
  void givenNullPort_shouldUseDefault() {
    // Create catalog config with null port
    CatalogServerProperties config = new CatalogServerProperties(null);

    assertThat(config.getPort()).isEqualTo(11051);
  }

  @Test
  void givenValidTlsConfig_shouldCreateConfig() {
    // Create TLS config
    ServerTlsProperties tls =
        new ServerTlsProperties(true, "/path/to/cert.pem", "/path/to/key.pem");

    assertThat(tls.isEnabled()).isTrue();
    assertThat(tls.getCertChainPath()).isEqualTo("/path/to/cert.pem");
    assertThat(tls.getPrivateKeyPath()).isEqualTo("/path/to/key.pem");
  }

  @Test
  void givenPlaintextConfig_shouldCreateConfig() {
    // Create plaintext config
    ServerTlsProperties tls = new ServerTlsProperties(false, null, null);

    assertThat(tls.isEnabled()).isFalse();
    assertThat(tls.getCertChainPath()).isNull();
    assertThat(tls.getPrivateKeyPath()).isNull();
  }

  @Test
  void givenTlsEnabledWithoutCertPath_shouldCreateInvalidConfig() {
    // Create TLS config without cert path (validation will fail at Spring level)
    ServerTlsProperties tls = new ServerTlsProperties(true, null, "/path/to/key.pem");

    assertThat(tls.isEnabled()).isTrue();
    assertThat(tls.getCertChainPath()).isNull();
    assertThat(tls.getPrivateKeyPath()).isEqualTo("/path/to/key.pem");
  }

  @Test
  void givenTlsEnabledWithoutKeyPath_shouldCreateInvalidConfig() {
    // Create TLS config without key path (validation will fail at Spring level)
    ServerTlsProperties tls = new ServerTlsProperties(true, "/path/to/cert.pem", null);

    assertThat(tls.isEnabled()).isTrue();
    assertThat(tls.getCertChainPath()).isEqualTo("/path/to/cert.pem");
    assertThat(tls.getPrivateKeyPath()).isNull();
  }
}
