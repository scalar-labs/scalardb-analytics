/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.auth;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * The type of authentication backend for password-based authentication.
 *
 * <p>This enum is part of the public API surface: the CLI, SDK, and proto layers all reference it
 * to describe which user-directory holds a credential.
 */
public enum PasswordBackendType {
  /** Built-in backend that stores credentials in the application database. */
  INTERNAL,

  /** External ScalarDB Cluster authentication backend. */
  SCALARDB_CLUSTER;

  /**
   * Parses a user-facing backend name into a {@link PasswordBackendType}. Matching is
   * case-insensitive against the enum constant name (e.g. {@code "internal"}, {@code "INTERNAL"},
   * and {@code "Internal"} all resolve to {@link #INTERNAL}).
   *
   * @throws IllegalArgumentException if {@code s} is {@code null}, empty, or does not match any
   *     enum constant; the message enumerates the accepted values
   */
  public static PasswordBackendType fromString(String s) {
    if (s != null && !s.isEmpty()) {
      try {
        return PasswordBackendType.valueOf(s.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException ignored) {
        // fall through to the unified error message below
      }
    }
    String accepted =
        Arrays.stream(values())
            .map(t -> t.name().toLowerCase(Locale.ROOT))
            .collect(Collectors.joining(", "));
    throw new IllegalArgumentException(
        "Unsupported backend: " + s + " (accepted values: " + accepted + ")");
  }
}
