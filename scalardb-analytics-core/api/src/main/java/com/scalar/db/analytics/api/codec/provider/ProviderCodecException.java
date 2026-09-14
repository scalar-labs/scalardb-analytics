/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.codec.provider;

/** Exception thrown when provider codec operations fail. */
public class ProviderCodecException extends RuntimeException {

  public ProviderCodecException(String message) {
    super(message);
  }

  public ProviderCodecException(String message, Throwable cause) {
    super(message, cause);
  }
}
