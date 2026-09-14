/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.lib.functional;

/**
 * Represents a supplier of results that might throw an exception.
 *
 * @param <R> the type of the return value from the function
 * @param <E> the type of the checked exception
 */
@FunctionalInterface
public interface ThrowableSupplier<R, E extends Throwable> {
  R get() throws E;
}
