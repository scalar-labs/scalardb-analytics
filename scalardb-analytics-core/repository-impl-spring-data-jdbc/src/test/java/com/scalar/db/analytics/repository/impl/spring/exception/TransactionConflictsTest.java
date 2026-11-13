/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.exception;

import static com.scalar.db.analytics.repository.impl.spring.constants.EntityTypes.CATALOG;
import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.sql.exception.TransactionConflictException;
import com.scalar.db.sql.exception.UnknownTransactionStatusException;
import com.scalar.db.sql.springdata.exception.ScalarDbTransientException;
import com.scalar.db.sql.springdata.exception.ScalarDbUnknownTransactionStateException;
import java.sql.SQLTransactionRollbackException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.PessimisticLockingFailureException;

class TransactionConflictsTest {

  private static final String TX_ID = "tx-1";

  /** The cause chain observed for a commit-time conflict (Spring → JDBC → ScalarDB SQL). */
  private static PessimisticLockingFailureException commitConflict() {
    return new PessimisticLockingFailureException(
        "JDBC commit; Transaction conflict",
        new SQLTransactionRollbackException(
            "Transaction conflict",
            "40000",
            new TransactionConflictException("DB-CORE-20022: anti-dependency", TX_ID)));
  }

  private final SpringDataJdbcErrorMapper errorMapper =
      new SpringDataJdbcErrorMapper(new ScalarDbSqlErrorAnalyzer());

  @Test
  void isConflict_shouldFindConflictInCauseChain() {
    assertThat(TransactionConflicts.isConflict(commitConflict())).isTrue();
    assertThat(
            TransactionConflicts.isConflict(
                new TransactionConflictException("DB-CORE-20013: conflict", TX_ID)))
        .isTrue();
  }

  @Test
  void isConflict_shouldNotTreatUnknownTransactionStatusAsConflict() {
    // The commit may have succeeded, so the request must not be retried automatically.
    assertThat(
            TransactionConflicts.isConflict(
                new ScalarDbUnknownTransactionStateException(
                    "unknown",
                    new UnknownTransactionStatusException("unknown transaction status", TX_ID),
                    TX_ID)))
        .isFalse();
  }

  @Test
  void isConflict_shouldReturnFalseForOtherExceptions() {
    assertThat(TransactionConflicts.isConflict(null)).isFalse();
    assertThat(TransactionConflicts.isConflict(new RuntimeException("other"))).isFalse();
    assertThat(
            TransactionConflicts.isConflict(
                new PessimisticLockingFailureException(
                    "JDBC commit", new SQLTransactionRollbackException("deadlock", "40001"))))
        .isFalse();
  }

  @Test
  void isConflict_shouldTerminateOnCyclicCauseChain() {
    RuntimeException first = new RuntimeException("first");
    RuntimeException second = new RuntimeException("second", first);
    first.initCause(second);

    assertThat(TransactionConflicts.isConflict(first)).isFalse();
  }

  @Test
  void toAnalyticsException_shouldUseRetryableConflictCodeAndKeepCause() {
    PessimisticLockingFailureException conflict = commitConflict();

    AnalyticsException mapped = TransactionConflicts.toAnalyticsException(conflict);

    assertThat(mapped.getErrorCode())
        .isEqualTo(AnalyticsErrorCode.ANALYTICS_DB_TRANSACTION_CONFLICT);
    assertThat(mapped.getErrorCode().isRetryable()).isTrue();
    assertThat(mapped.getCause()).isSameAs(conflict);
  }

  @Test
  void errorMapper_shouldMapStatementTimeConflictToTransactionConflict() {
    // A conflict raised by a statement reaches the repository as the ScalarDB SQL translator's
    // TransientDataAccessException, which would otherwise map to ANALYTICS_DB_CONNECTION_FAILED.
    ScalarDbTransientException statementConflict =
        new ScalarDbTransientException(
            "Transaction conflict",
            new SQLTransactionRollbackException(
                "Transaction conflict",
                "40000",
                new TransactionConflictException("DB-CORE-20013: conflict", TX_ID)),
            TX_ID);

    assertThat(errorMapper.mapException(statementConflict, CATALOG, "c").getErrorCode())
        .isEqualTo(AnalyticsErrorCode.ANALYTICS_DB_TRANSACTION_CONFLICT);
  }

  @Test
  void errorMapper_shouldMapConflictWrappedInNonDataAccessException() {
    Exception wrapped =
        new IllegalStateException(
            "wrapped", new TransactionConflictException("DB-CORE-20013: conflict", TX_ID));

    assertThat(errorMapper.mapException(wrapped, CATALOG, "c").getErrorCode())
        .isEqualTo(AnalyticsErrorCode.ANALYTICS_DB_TRANSACTION_CONFLICT);
  }

  @Test
  void errorMapper_shouldKeepMappingOtherTransientErrorsToConnectionFailure() {
    ScalarDbTransientException other =
        new ScalarDbTransientException(
            "timeout", new SQLTransactionRollbackException("timeout", "40000"), TX_ID);

    assertThat(errorMapper.mapException(other, CATALOG, "c").getErrorCode())
        .isEqualTo(AnalyticsErrorCode.ANALYTICS_DB_CONNECTION_FAILED);
  }
}
