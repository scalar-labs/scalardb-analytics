/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository;

import com.scalar.db.analytics.lib.functional.ThrowableFunction;
import org.jspecify.annotations.Nullable;

/** Represents a transaction manager for the repository. */
public interface RepositoryTransactionManager<T extends RepositoryTransactionContext> {
  /**
   * Executes the given function within a single transaction if transactions are supported. This is
   * intended to be used to execute multiple repository operations within a single transaction.
   *
   * @param f the function to execute that receives a transaction context
   * @param <R> the type of the return value from the function
   * @param <E> the type of the checked exception
   * @return the result of the function
   * @throws E if the function throws an exception
   */
  <R extends @Nullable Object, E extends Throwable> R withTransaction(ThrowableFunction<T, R, E> f)
      throws E;

  /**
   * Creates a new transaction context to execute a single repository operation. The returned
   * context must not be shared among multiple operations.
   *
   * @return a new transaction context
   */
  T single();
}
