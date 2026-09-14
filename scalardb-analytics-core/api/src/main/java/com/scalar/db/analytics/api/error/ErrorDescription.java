/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.error;

import com.google.errorprone.annotations.Immutable;
import lombok.Value;

/**
 * Static, user-facing description of an {@link AnalyticsErrorCode}.
 *
 * <p>Colocated with the error code constant so that the (code, message, cause, action) tuple lives
 * in a single place. Throw sites pass only the code and contextual {@code metadata}; the wording is
 * never written at the throw site.
 *
 * <p><b>The text content is not part of the API contract.</b> Consumers must not parse or branch on
 * the wording — it may be reworded, translated, or restructured at any time. Programmatic control
 * flow uses the error code itself or {@link AnalyticsErrorCode.ErrorCategory} (see ADR-0010).
 *
 * @see AnalyticsErrorCode
 */
@Immutable
@Value
public class ErrorDescription {

  /**
   * Short, fixed human-readable description (e.g. {@code "Access denied"}).
   *
   * <p>Used as the message in the formatted {@code AnalyticsException} output. Per-invocation
   * detail (entity names, IDs) is conveyed via the exception's metadata map, not by templating this
   * string.
   */
  String message;

  /**
   * Why the error occurs (e.g. {@code "The authenticated user lacks the required role..."}).
   *
   * <p>Intended for user-facing documentation. Not included in the default exception message.
   */
  String cause;

  /**
   * What the user can do to resolve the error (e.g. {@code "Grant the user the necessary
   * role..."}).
   *
   * <p>Intended for user-facing documentation. Not included in the default exception message.
   */
  String action;
}
