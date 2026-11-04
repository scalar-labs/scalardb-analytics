/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.config;

import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * A client configuration field that can be read from environment variables and configuration files.
 *
 * <p>Each key has a property key used in {@code client.properties} (for example {@code
 * scalar.db.analytics.client.auth.username}) and an environment variable name derived from it by
 * upper-casing it and replacing dots with underscores (for example {@code
 * SCALAR_DB_ANALYTICS_CLIENT_AUTH_USERNAME}). Integrations that use their own property names, such
 * as the Spark connector, can map keys through {@link #relativeKey()}.
 */
public enum ClientConfigKey {
  /** The username for password authentication. Blank values are treated as unset. */
  AUTH_USERNAME("auth.username", true),

  /**
   * The password for password authentication. Values are used as is: they are not trimmed, and an
   * empty value counts as set.
   */
  AUTH_PASSWORD("auth.password", false);

  /** The prefix of every property key in {@code client.properties}. */
  public static final String PROPERTY_PREFIX = "scalar.db.analytics.client.";

  private final String relativeKey;
  private final boolean blankAsUnset;

  ClientConfigKey(String relativeKey, boolean blankAsUnset) {
    this.relativeKey = relativeKey;
    this.blankAsUnset = blankAsUnset;
  }

  /** Returns the key without {@link #PROPERTY_PREFIX}, for example {@code auth.username}. */
  public String relativeKey() {
    return relativeKey;
  }

  /** Returns the property key, for example {@code scalar.db.analytics.client.auth.username}. */
  public String propertyKey() {
    return PROPERTY_PREFIX + relativeKey;
  }

  /**
   * Returns the environment variable name, for example {@code
   * SCALAR_DB_ANALYTICS_CLIENT_AUTH_USERNAME}.
   */
  public String envVarName() {
    return propertyKey().toUpperCase(Locale.ROOT).replace('.', '_');
  }

  /**
   * Normalizes a raw value read from a single source. Returns {@code null} when the source should
   * be treated as not defining this key, so that resolution falls through to the next source.
   */
  @Nullable String normalize(@Nullable String raw) {
    if (raw == null || !blankAsUnset) {
      return raw;
    }
    String trimmed = raw.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
