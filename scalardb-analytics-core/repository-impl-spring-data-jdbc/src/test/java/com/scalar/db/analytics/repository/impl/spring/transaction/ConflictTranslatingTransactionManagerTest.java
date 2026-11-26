/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.sql.exception.TransactionConflictException;
import java.sql.SQLException;
import java.sql.SQLTransactionRollbackException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

class ConflictTranslatingTransactionManagerTest {

  private final PlatformTransactionManager delegate = mock(PlatformTransactionManager.class);
  private final ConflictTranslatingTransactionManager transactionManager =
      new ConflictTranslatingTransactionManager(() -> delegate);
  private final TransactionStatus status = new SimpleTransactionStatus();

  @Test
  void constructor_shouldNotResolveDelegate() {
    new ConflictTranslatingTransactionManager(
        () -> {
          throw new AssertionError("resolved at construction");
        });
  }

  @Test
  void getTransactionAndRollback_shouldDelegate() {
    TransactionDefinition definition = new DefaultTransactionDefinition();
    when(delegate.getTransaction(definition)).thenReturn(status);

    assertThat(transactionManager.getTransaction(definition)).isSameAs(status);
    transactionManager.rollback(status);

    verify(delegate).rollback(status);
  }

  @Test
  void commit_shouldDelegate() {
    transactionManager.commit(status);

    verify(delegate).commit(status);
  }

  @Test
  void commit_shouldReportConflictAsRetryableTransactionConflict() {
    PessimisticLockingFailureException conflict =
        new PessimisticLockingFailureException(
            "JDBC commit; Transaction conflict",
            new SQLTransactionRollbackException(
                "Transaction conflict",
                "40000",
                new TransactionConflictException("DB-CORE-20013: conflict", "tx-1")));
    doThrow(conflict).when(delegate).commit(status);

    assertThatThrownBy(() -> transactionManager.commit(status))
        .isInstanceOfSatisfying(
            AnalyticsException.class,
            e -> {
              assertThat(e.getErrorCode())
                  .isEqualTo(AnalyticsErrorCode.ANALYTICS_DB_TRANSACTION_CONFLICT);
              assertThat(e.getCause()).isSameAs(conflict);
            });
  }

  @Test
  void commit_shouldRethrowOtherFailuresUnchanged() {
    DataAccessResourceFailureException failure =
        new DataAccessResourceFailureException("JDBC commit", new SQLException("connection lost"));
    doThrow(failure).when(delegate).commit(status);

    assertThatThrownBy(() -> transactionManager.commit(status)).isSameAs(failure);
  }
}
