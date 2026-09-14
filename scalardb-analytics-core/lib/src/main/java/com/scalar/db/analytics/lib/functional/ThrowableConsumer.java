/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.lib.functional;

/**
 * Represents a consumer of a single input argument that can throw a checked exception.
 *
 * @param <T> the type of the input to the function
 * @param <E> the type of the checked exception
 */
@FunctionalInterface
public interface ThrowableConsumer<T, E extends Throwable> {
  void accept(T t) throws E;
}
