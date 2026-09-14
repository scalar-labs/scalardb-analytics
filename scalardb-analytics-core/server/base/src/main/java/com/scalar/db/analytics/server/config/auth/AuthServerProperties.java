/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.config.auth;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@ConfigurationProperties(prefix = "scalar.db.analytics.server.auth")
@Validated
public class AuthServerProperties {

  private static final long DEFAULT_TOKEN_TTL_SECONDS = 86400L; // 24 hours

  private final boolean enabled;
  @Valid private final PasswordAuthProperties password;
  private final @Nullable String initialAdminUsername;

  public AuthServerProperties(
      @Nullable Boolean enabled,
      @Nullable PasswordAuthProperties password,
      @Nullable String initialAdminUsername) {
    this.enabled = enabled != null ? enabled : false;
    this.password = password != null ? password : new PasswordAuthProperties(null, null, null);
    this.initialAdminUsername = initialAdminUsername;
  }

  @Getter
  public static class PasswordAuthProperties {
    private final PasswordBackendType backend;

    @Min(1)
    private final long tokenTtlSeconds;

    @Valid private final InternalProperties internal;

    public PasswordAuthProperties(
        @Nullable PasswordBackendType backend,
        @Nullable Long tokenTtlSeconds,
        @Nullable InternalProperties internal) {
      this.backend = backend != null ? backend : PasswordBackendType.INTERNAL;
      this.tokenTtlSeconds = tokenTtlSeconds != null ? tokenTtlSeconds : DEFAULT_TOKEN_TTL_SECONDS;
      this.internal = internal != null ? internal : new InternalProperties(null);
    }

    @Getter
    public static class InternalProperties {
      private final @Nullable String initialAdminPassword;

      public InternalProperties(@Nullable String initialAdminPassword) {
        this.initialAdminPassword = initialAdminPassword;
      }
    }
  }
}
