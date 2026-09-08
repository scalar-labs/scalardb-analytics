/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.retry;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * Configuration for gRPC retry behavior with exponential backoff.
 *
 * <p>This is an immutable value object that controls how the client retries transient gRPC errors.
 * By default, retry is enabled with sensible defaults (UNAVAILABLE, DEADLINE_EXCEEDED, and
 * RESOURCE_EXHAUSTED are retried with exponential backoff). Use {@link #disabled()} to create a
 * configuration that disables retry.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Use defaults (5 attempts, 100ms initial backoff, 5s max backoff, 2x multiplier)
 * RetryConfig config = RetryConfig.defaultConfig();
 *
 * // Custom configuration
 * RetryConfig config = RetryConfig.builder()
 *     .maxAttempts(3)
 *     .initialBackoffMillis(200)
 *     .maxBackoffMillis(10000)
 *     .backoffMultiplier(3.0)
 *     .build();
 *
 * // Disable retry
 * RetryConfig config = RetryConfig.disabled();
 * }</pre>
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RetryConfig {

  private static final int DEFAULT_MAX_ATTEMPTS = 5;
  private static final long DEFAULT_INITIAL_BACKOFF_MILLIS = 100;
  private static final long DEFAULT_MAX_BACKOFF_MILLIS = 5000;
  private static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;

  int maxAttempts;
  long initialBackoffMillis;
  long maxBackoffMillis;
  double backoffMultiplier;
  boolean enabled;

  /** Returns a default retry configuration with retry enabled. */
  public static RetryConfig defaultConfig() {
    return builder().build();
  }

  /** Returns a retry configuration with retry disabled. */
  public static RetryConfig disabled() {
    return new Builder().enabled(false).build();
  }

  /** Returns a new builder for constructing a RetryConfig. */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Converts this configuration to a gRPC service config map suitable for {@link
   * io.grpc.ManagedChannelBuilder#defaultServiceConfig(Map)}.
   *
   * @return the service config as a nested map
   */
  public Map<String, Object> toServiceConfig() {
    Map<String, Object> retryPolicy = new HashMap<>();
    retryPolicy.put("maxAttempts", (double) maxAttempts);
    retryPolicy.put("initialBackoff", millisToGrpcDuration(initialBackoffMillis));
    retryPolicy.put("maxBackoff", millisToGrpcDuration(maxBackoffMillis));
    retryPolicy.put("backoffMultiplier", backoffMultiplier);
    retryPolicy.put("retryableStatusCodes", RETRYABLE_STATUS_CODES);

    // Empty name matches all services/methods
    Map<String, Object> name = new HashMap<>();
    name.put("service", "");

    Map<String, Object> methodConfig = new HashMap<>();
    methodConfig.put("name", Collections.singletonList(name));
    methodConfig.put("retryPolicy", retryPolicy);

    Map<String, Object> serviceConfig = new HashMap<>();
    serviceConfig.put("methodConfig", Collections.singletonList(methodConfig));
    return serviceConfig;
  }

  private static String millisToGrpcDuration(long millis) {
    if (millis % 1000 == 0) {
      return (millis / 1000) + "s";
    }
    return String.format(Locale.ROOT, "%.3fs", millis / 1000.0);
  }

  private static final List<String> RETRYABLE_STATUS_CODES =
      Collections.unmodifiableList(
          Arrays.asList("UNAVAILABLE", "DEADLINE_EXCEEDED", "RESOURCE_EXHAUSTED"));

  /** Builder for constructing RetryConfig instances. */
  public static final class Builder {
    private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
    private long initialBackoffMillis = DEFAULT_INITIAL_BACKOFF_MILLIS;
    private long maxBackoffMillis = DEFAULT_MAX_BACKOFF_MILLIS;
    private double backoffMultiplier = DEFAULT_BACKOFF_MULTIPLIER;
    private boolean enabled = true;

    private Builder() {}

    public Builder maxAttempts(int maxAttempts) {
      this.maxAttempts = maxAttempts;
      return this;
    }

    public Builder initialBackoffMillis(long initialBackoffMillis) {
      this.initialBackoffMillis = initialBackoffMillis;
      return this;
    }

    public Builder maxBackoffMillis(long maxBackoffMillis) {
      this.maxBackoffMillis = maxBackoffMillis;
      return this;
    }

    public Builder backoffMultiplier(double backoffMultiplier) {
      this.backoffMultiplier = backoffMultiplier;
      return this;
    }

    public Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public RetryConfig build() {
      if (enabled) {
        if (maxAttempts < 1) {
          throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        if (initialBackoffMillis < 0) {
          throw new IllegalArgumentException("initialBackoffMillis must be non-negative");
        }
        if (maxBackoffMillis < 0) {
          throw new IllegalArgumentException("maxBackoffMillis must be non-negative");
        }
        if (maxBackoffMillis < initialBackoffMillis) {
          throw new IllegalArgumentException(
              "maxBackoffMillis must be greater than or equal to initialBackoffMillis");
        }
        if (backoffMultiplier < 1.0) {
          throw new IllegalArgumentException("backoffMultiplier must be at least 1.0");
        }
      }
      return new RetryConfig(
          maxAttempts, initialBackoffMillis, maxBackoffMillis, backoffMultiplier, enabled);
    }
  }
}
