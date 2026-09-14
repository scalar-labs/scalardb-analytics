/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.config;

import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@ConfigurationProperties(prefix = "scalar.db.analytics.server.tls")
@Validated
public class ServerTlsProperties {
  private static final boolean DEFAULT_ENABLED = false;

  private final boolean enabled;
  @Nullable private final String certChainPath;
  @Nullable private final String privateKeyPath;

  public ServerTlsProperties(
      @Nullable Boolean enabled, @Nullable String certChainPath, @Nullable String privateKeyPath) {
    this.enabled = enabled != null ? enabled : DEFAULT_ENABLED;
    this.certChainPath = certChainPath;
    this.privateKeyPath = privateKeyPath;
  }

  @SuppressWarnings("unused")
  @AssertTrue(message = "TLS cert chain path is required when TLS is enabled")
  private boolean isCertChainPathValid() {
    return !enabled || (certChainPath != null && !certChainPath.isEmpty());
  }

  @SuppressWarnings("unused")
  @AssertTrue(message = "TLS private key path is required when TLS is enabled")
  private boolean isPrivateKeyPathValid() {
    return !enabled || (privateKeyPath != null && !privateKeyPath.isEmpty());
  }
}
