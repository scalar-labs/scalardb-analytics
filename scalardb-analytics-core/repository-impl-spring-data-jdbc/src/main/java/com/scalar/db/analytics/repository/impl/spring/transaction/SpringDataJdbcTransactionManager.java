/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.transaction;

import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.lib.functional.ThrowableFunction;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.impl.spring.exception.TransactionConflicts;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class SpringDataJdbcTransactionManager
    implements RepositoryTransactionManager<SpringDataJdbcTransactionContext> {

  private final TransactionTemplate transactionTemplate;

  public SpringDataJdbcTransactionManager(TransactionTemplate transactionTemplate) {
    this.transactionTemplate = transactionTemplate;
  }

  /**
   * {@inheritDoc}
   *
   * <p>An exception thrown by {@code f} is rethrown unchanged after the rollback. A transaction
   * conflict reported by the commit is thrown as {@link
   * com.scalar.db.analytics.api.error.AnalyticsErrorCode#ANALYTICS_DB_TRANSACTION_CONFLICT}; any
   * other failure of the transaction manager itself is rethrown unchanged.
   */
  @Override
  public <R extends @Nullable Object, E extends Throwable> R withTransaction(
      ThrowableFunction<SpringDataJdbcTransactionContext, R, E> f) throws E {
    SpringDataJdbcTransactionContext context = new SpringDataJdbcTransactionContext();

    try {
      return transactionTemplate.execute(
          status -> {
            try {
              return f.apply(context);
            } catch (Throwable e) {
              status.setRollbackOnly();
              throw new FunctionFailure(e);
            }
          });
    } catch (FunctionFailure e) {
      @SuppressWarnings("unchecked")
      E failure = (E) e.failure;
      throw failure;
    } catch (RuntimeException e) {
      // Thrown by the transaction manager itself, typically by the commit. Only the function's own
      // exceptions are unwrapped above: unwrapping these too would replace Spring's exception with
      // its JDBC cause.
      if (!(e instanceof AnalyticsException) && TransactionConflicts.isConflict(e)) {
        throw TransactionConflicts.toAnalyticsException(e);
      }
      throw e;
    }
  }

  @Override
  public SpringDataJdbcTransactionContext single() {
    // For Spring Data JDBC, we return a context that can be used for single operations.
    // The actual transaction boundary will be managed by Spring's @Transactional annotation
    // on the repository methods.
    return new SpringDataJdbcTransactionContext();
  }

  /** Carries an exception thrown by the function out of the transaction callback. */
  private static final class FunctionFailure extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final Throwable failure;

    FunctionFailure(Throwable failure) {
      super(failure);
      this.failure = failure;
    }
  }
}
