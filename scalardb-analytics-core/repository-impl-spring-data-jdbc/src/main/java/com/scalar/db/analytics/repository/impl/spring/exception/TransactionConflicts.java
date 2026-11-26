/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.exception;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.sql.exception.TransactionConflictException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Recognizes ScalarDB transaction conflicts and reports them as {@link
 * AnalyticsErrorCode#ANALYTICS_DB_TRANSACTION_CONFLICT}.
 *
 * <p>ScalarDB SQL raises {@link TransactionConflictException} when a transaction conflicts with a
 * concurrent one: a write-write conflict, or, under SERIALIZABLE, a read that a concurrent commit
 * invalidated (an anti-dependency). The transaction is aborted as a whole, so the request can be
 * retried. The exception reaches this code wrapped by the JDBC driver ({@code
 * SQLTransactionRollbackException}) and by Spring ({@code PessimisticLockingFailureException} at
 * commit, a {@code TransientDataAccessException} during a statement), so the cause chain is
 * searched.
 *
 * <p>An unknown transaction status ({@code UnknownTransactionStatusException}) is deliberately not
 * a conflict: the commit may have succeeded, so retrying could apply the request twice.
 */
public final class TransactionConflicts {

  private TransactionConflicts() {}

  /** Returns whether {@code e} or one of its causes is a ScalarDB SQL transaction conflict. */
  public static boolean isConflict(@Nullable Throwable e) {
    Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    for (Throwable current = e;
        current != null && seen.add(current);
        current = current.getCause()) {
      if (current instanceof TransactionConflictException) {
        return true;
      }
    }
    return false;
  }

  /** Returns the retryable error that reports the conflict {@code e}. */
  public static AnalyticsException toAnalyticsException(Throwable e) {
    return new AnalyticsException(AnalyticsErrorCode.ANALYTICS_DB_TRANSACTION_CONFLICT, e);
  }
}
