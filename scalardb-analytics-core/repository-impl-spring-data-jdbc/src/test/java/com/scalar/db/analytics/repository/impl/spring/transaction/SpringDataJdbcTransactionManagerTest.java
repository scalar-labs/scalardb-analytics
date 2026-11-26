/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.sql.exception.TransactionConflictException;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLTransactionRollbackException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class SpringDataJdbcTransactionManagerTest {

  private PlatformTransactionManager platformTransactionManager;
  private SpringDataJdbcTransactionManager transactionManager;

  @BeforeEach
  void setUp() {
    platformTransactionManager = mock(PlatformTransactionManager.class);
    when(platformTransactionManager.getTransaction(any()))
        .thenReturn(new SimpleTransactionStatus());
    transactionManager =
        new SpringDataJdbcTransactionManager(new TransactionTemplate(platformTransactionManager));
  }

  @Test
  void withTransaction_shouldReturnResultAndCommit() {
    String result = transactionManager.withTransaction(ctx -> "result");

    assertThat(result).isEqualTo("result");
    verify(platformTransactionManager).commit(any());
  }

  @Test
  void withTransaction_shouldRethrowCheckedExceptionOfFunctionUnchangedAndRollBack() {
    IOException failure = new IOException("io");

    assertThatThrownBy(
            () ->
                transactionManager.withTransaction(
                    ctx -> {
                      throw failure;
                    }))
        .isSameAs(failure);
    verify(platformTransactionManager).rollback(any());
    verify(platformTransactionManager, never()).commit(any());
  }

  @Test
  void withTransaction_shouldRethrowRuntimeExceptionWithCauseOfFunctionUnchanged() {
    AnalyticsException failure =
        new AnalyticsException(
            AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED, new SQLException("cause"));

    assertThatThrownBy(
            () ->
                transactionManager.withTransaction(
                    ctx -> {
                      throw failure;
                    }))
        .isSameAs(failure);
  }

  @Test
  void withTransaction_shouldReportCommitConflictAsRetryableTransactionConflict() {
    PessimisticLockingFailureException conflict =
        new PessimisticLockingFailureException(
            "JDBC commit; Transaction conflict",
            new SQLTransactionRollbackException(
                "Transaction conflict",
                "40000",
                new TransactionConflictException("DB-CORE-20022: anti-dependency", "tx-1")));
    doThrow(conflict).when(platformTransactionManager).commit(any());

    assertThatThrownBy(() -> transactionManager.withTransaction(ctx -> "result"))
        .isInstanceOfSatisfying(
            AnalyticsException.class,
            e -> {
              assertThat(e.getErrorCode())
                  .isEqualTo(AnalyticsErrorCode.ANALYTICS_DB_TRANSACTION_CONFLICT);
              assertThat(e.getCause()).isSameAs(conflict);
            });
  }

  @Test
  void withTransaction_shouldRethrowOtherCommitFailureWithoutUnwrappingIt() {
    DataAccessResourceFailureException failure =
        new DataAccessResourceFailureException("JDBC commit", new SQLException("connection lost"));
    doThrow(failure).when(platformTransactionManager).commit(any());

    assertThatThrownBy(() -> transactionManager.withTransaction(ctx -> "result")).isSameAs(failure);
  }
}
