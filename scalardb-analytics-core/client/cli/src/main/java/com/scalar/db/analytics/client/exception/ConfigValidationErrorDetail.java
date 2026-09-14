/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.exception;

public enum ConfigValidationErrorDetail {
  REQUIRED_KEY_NOT_SET("Required key is not set");

  private final String message;

  ConfigValidationErrorDetail(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
