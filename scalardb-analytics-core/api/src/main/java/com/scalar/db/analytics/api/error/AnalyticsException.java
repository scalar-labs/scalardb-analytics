/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.error;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Unified exception for all ScalarDB Analytics error scenarios.
 *
 * <p>Every instance carries an {@link AnalyticsErrorCode} that classifies the failure and maps to a
 * transport-specific status code (e.g., gRPC status). The user-facing message is sourced from the
 * code's colocated {@link ErrorDescription}; throw sites contribute only contextual {@code
 * metadata}, never message strings.
 *
 * <p>The formatted message includes the error code prefix and, when metadata is present, a
 * bracketed key-value list:
 *
 * <pre>{@code DB-ANALYTICS-10003: Access denied [resource=catalog/foo, user_id=alice]}</pre>
 *
 * <p>This is an unchecked exception to eliminate forced wrapping at layer boundaries.
 */
public class AnalyticsException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final AnalyticsErrorCode errorCode;
  private final Map<String, String> metadata;

  /**
   * Constructs a new AnalyticsException with no metadata.
   *
   * @param errorCode the error code classifying this failure
   */
  public AnalyticsException(AnalyticsErrorCode errorCode) {
    super(formatMessage(errorCode, Collections.emptyMap()));
    this.errorCode = errorCode;
    this.metadata = Collections.emptyMap();
  }

  /**
   * Constructs a new AnalyticsException with no metadata, wrapping a cause.
   *
   * @param errorCode the error code classifying this failure
   * @param cause the underlying cause
   */
  public AnalyticsException(AnalyticsErrorCode errorCode, Throwable cause) {
    super(formatMessage(errorCode, Collections.emptyMap()), cause);
    this.errorCode = errorCode;
    this.metadata = Collections.emptyMap();
  }

  /**
   * Constructs a new AnalyticsException with metadata.
   *
   * @param errorCode the error code classifying this failure
   * @param metadata structured key-value pairs for programmatic access
   */
  public AnalyticsException(AnalyticsErrorCode errorCode, Map<String, String> metadata) {
    super(formatMessage(errorCode, metadata));
    this.errorCode = errorCode;
    this.metadata = Collections.unmodifiableMap(new HashMap<>(metadata));
  }

  /**
   * Constructs a new AnalyticsException with metadata and a cause.
   *
   * @param errorCode the error code classifying this failure
   * @param metadata structured key-value pairs for programmatic access
   * @param cause the underlying cause
   */
  public AnalyticsException(
      AnalyticsErrorCode errorCode, Map<String, String> metadata, Throwable cause) {
    super(formatMessage(errorCode, metadata), cause);
    this.errorCode = errorCode;
    this.metadata = Collections.unmodifiableMap(new HashMap<>(metadata));
  }

  /** Returns the error code classifying this failure. */
  public AnalyticsErrorCode getErrorCode() {
    return errorCode;
  }

  /** Returns the structured metadata associated with this error. */
  public Map<String, String> getMetadata() {
    return metadata;
  }

  /**
   * Returns the formatted message including the error code prefix.
   *
   * <p>Equivalent to {@link #getMessage()} but named explicitly for clarity.
   *
   * @return a message like {@code "DB-ANALYTICS-10100: Catalog already exists [catalog_name=foo]"}
   */
  public String getFormattedMessage() {
    return getMessage();
  }

  private static String formatMessage(AnalyticsErrorCode errorCode, Map<String, String> metadata) {
    StringBuilder sb = new StringBuilder();
    sb.append(errorCode.getCode()).append(": ").append(errorCode.getDescription().getMessage());
    if (!metadata.isEmpty()) {
      StringJoiner joiner = new StringJoiner(", ", " [", "]");
      metadata.entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(e -> joiner.add(e.getKey() + "=" + e.getValue()));
      sb.append(joiner);
    }
    return sb.toString();
  }
}
