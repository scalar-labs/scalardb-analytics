/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.lib.exception;

public class NonExhaustiveEnumSwitchException extends RuntimeException {
  private static final long serialVersionUID = -5124387623087063146L;

  public <T> NonExhaustiveEnumSwitchException(Class<T> clazz, T value) {
    super(getMessageForClass(clazz, value));
  }

  private static <T> String getMessageForClass(Class<T> clazz, T value) {
    return "Non-exhaustive switch statement on enum "
        + clazz.getSimpleName()
        + " for value "
        + value;
  }
}
