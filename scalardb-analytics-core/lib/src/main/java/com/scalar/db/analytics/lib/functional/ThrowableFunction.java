/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.lib.functional;

import org.jspecify.annotations.Nullable;

/**
 * Represents a function that accepts one argument and produces a result. This function can throw a
 * checked exception.
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the return value from the function
 * @param <E> the type of the checked exception
 */
@FunctionalInterface
public interface ThrowableFunction<
    T extends @Nullable Object, R extends @Nullable Object, E extends Throwable> {
  R apply(T t) throws E;
}
