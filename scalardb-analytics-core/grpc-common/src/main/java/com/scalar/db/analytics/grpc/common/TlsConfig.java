/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.common;

import lombok.Builder;
import lombok.Value;
import org.jspecify.annotations.Nullable;

/** Configuration for TLS/SSL settings. */
@Value
@Builder
public class TlsConfig {
  /**
   * Path to a custom CA root certificate file.
   *
   * <p>If not set, the system's default trust certificates will be used.
   */
  @Nullable String caRootCertPath;

  /**
   * Authority to use for TLS hostname verification.
   *
   * <p>This is useful when the server's certificate doesn't match the hostname used to connect.
   */
  @Nullable String overrideAuthority;
}
