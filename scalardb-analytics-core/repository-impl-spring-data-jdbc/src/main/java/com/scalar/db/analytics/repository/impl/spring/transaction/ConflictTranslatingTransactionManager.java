/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.transaction;

import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.repository.impl.spring.exception.TransactionConflicts;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

/**
 * A {@link PlatformTransactionManager} that reports a transaction conflict at commit as {@link
 * com.scalar.db.analytics.api.error.AnalyticsErrorCode#ANALYTICS_DB_TRANSACTION_CONFLICT}.
 *
 * <p>The {@code @Transactional} repository methods commit in Spring's transaction interceptor when
 * they are not called within {@link SpringDataJdbcTransactionManager#withTransaction} (that is,
 * with {@link SpringDataJdbcTransactionManager#single()}). The commit then fails outside the
 * repository's error mapping, so this decorator, registered as the annotation-driven transaction
 * manager, translates the conflict there. Everything else is delegated unchanged.
 */
public final class ConflictTranslatingTransactionManager implements PlatformTransactionManager {

  private final PlatformTransactionManager delegate;

  public ConflictTranslatingTransactionManager(PlatformTransactionManager delegate) {
    this.delegate = delegate;
  }

  @Override
  public TransactionStatus getTransaction(@Nullable TransactionDefinition definition) {
    return delegate.getTransaction(definition);
  }

  @Override
  public void commit(TransactionStatus status) {
    try {
      delegate.commit(status);
    } catch (RuntimeException e) {
      if (!(e instanceof AnalyticsException) && TransactionConflicts.isConflict(e)) {
        throw TransactionConflicts.toAnalyticsException(e);
      }
      throw e;
    }
  }

  @Override
  public void rollback(TransactionStatus status) {
    delegate.rollback(status);
  }
}
