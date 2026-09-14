/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.exception;

public class ClientException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final ErrorDetail detail;

  public ClientException(ErrorDetail detail) {
    super(detail.getMessage(), detail.getCause());
    this.detail = detail;
  }

  public ErrorDetail detail() {
    return detail;
  }
}
