/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.transaction;

import com.scalar.db.analytics.lib.functional.ThrowableFunction;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
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
              throw new RuntimeException(e);
            }
          });
    } catch (RuntimeException e) {
      if (e.getCause() != null) {
        @SuppressWarnings("unchecked")
        E cause = (E) e.getCause();
        throw cause;
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
}
