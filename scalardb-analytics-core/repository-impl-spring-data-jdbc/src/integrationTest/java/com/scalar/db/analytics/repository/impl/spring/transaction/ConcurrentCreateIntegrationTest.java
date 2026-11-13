/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.domain.authz.Role;
import com.scalar.db.analytics.repository.impl.spring.repository.authz.RoleRepositoryImpl;
import com.scalar.db.analytics.repository.impl.spring.support.AbstractScalarDbIntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionExecution;
import org.springframework.transaction.TransactionExecutionListener;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

/**
 * Two requests that create an entity with the same name at the same time, each checking that the
 * name is free and then inserting in one transaction.
 *
 * <p>Both transactions finish the lookup and the insert before either commits: a listener on the
 * transaction manager holds each one at a barrier just before its commit. Under the SNAPSHOT
 * default that the server used before, both committed and left a duplicate that could no longer be
 * looked up by name. With the server's properties (SERIALIZABLE), at most one commits and the other
 * fails with the retryable {@code ANALYTICS_DB_TRANSACTION_CONFLICT}; retrying it reports {@code
 * *_ALREADY_EXISTS}.
 *
 * <p>Both commit paths are covered: {@link SpringDataJdbcTransactionManager#withTransaction}, and a
 * repository method called with {@link SpringDataJdbcTransactionManager#single()}, which commits in
 * the {@code @Transactional} interceptor (catalog and role creation use it).
 */
class ConcurrentCreateIntegrationTest extends AbstractScalarDbIntegrationTest {

  private static final int MAX_RETRIES = 5;

  /**
   * The barrier of the current test thread; null outside the races, so other tests are unaffected.
   */
  private static final ThreadLocal<@Nullable CyclicBarrier> BEFORE_COMMIT = new ThreadLocal<>();

  private static final TransactionExecutionListener HOLD_BEFORE_COMMIT =
      new TransactionExecutionListener() {
        @Override
        public void beforeCommit(TransactionExecution transaction) {
          CyclicBarrier barrier = BEFORE_COMMIT.get();
          if (barrier == null) {
            return;
          }
          try {
            barrier.await(30, TimeUnit.SECONDS);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
          } catch (BrokenBarrierException | TimeoutException e) {
            throw new IllegalStateException(e);
          }
        }
      };

  @Autowired private List<PlatformTransactionManager> platformTransactionManagers;
  @Autowired private RoleRepositoryImpl roleRepository;

  @BeforeAll
  void addListener() {
    for (PlatformTransactionManager manager : platformTransactionManagers) {
      if (manager instanceof AbstractPlatformTransactionManager abstractManager) {
        abstractManager.addListener(HOLD_BEFORE_COMMIT);
      }
    }
  }

  @AfterAll
  void removeListener() {
    for (PlatformTransactionManager manager : platformTransactionManagers) {
      if (manager instanceof AbstractPlatformTransactionManager abstractManager) {
        abstractManager.getTransactionExecutionListeners().remove(HOLD_BEFORE_COMMIT);
      }
    }
  }

  @Test
  void concurrentCatalogCreatesWithinWithTransaction_shouldNotCreateDuplicate() throws Exception {
    Attempt create =
        () ->
            transactionManager.withTransaction(
                ctx -> {
                  catalogRepository.create(ctx, Catalog.create("dup"));
                  return null;
                });

    assertNoDuplicate(
        create,
        AnalyticsErrorCode.CATALOG_ALREADY_EXISTS,
        () -> catalogRepository.findByName(transactionManager.single(), "dup"));
  }

  @Test
  void concurrentCatalogCreatesWithSingle_shouldNotCreateDuplicate() throws Exception {
    Attempt create =
        () -> catalogRepository.create(transactionManager.single(), Catalog.create("dup"));

    assertNoDuplicate(
        create,
        AnalyticsErrorCode.CATALOG_ALREADY_EXISTS,
        () -> catalogRepository.findByName(transactionManager.single(), "dup"));
  }

  @Test
  void concurrentRoleCreatesWithSingle_shouldNotCreateDuplicate() throws Exception {
    Attempt create = () -> roleRepository.create(transactionManager.single(), Role.create("dup"));

    assertNoDuplicate(
        create,
        AnalyticsErrorCode.ROLE_ALREADY_EXISTS,
        () -> roleRepository.findByName(transactionManager.single(), "dup"));
  }

  /**
   * Races two {@code create} attempts, then retries each loser as the client SDK would, and checks
   * that exactly one request created the entity and the other ended with {@code alreadyExists}.
   */
  private void assertNoDuplicate(
      Attempt create, AnalyticsErrorCode alreadyExists, Lookup lookupByName) throws Exception {
    List<@Nullable AnalyticsErrorCode> raced = race(create, create);

    // The race itself: each attempt commits or fails with the retryable conflict, never both
    // commit, and the name stays resolvable (a duplicate would make the lookup fail).
    assertThat(raced)
        .allSatisfy(
            outcome ->
                assertThat(outcome)
                    .isIn(null, AnalyticsErrorCode.ANALYTICS_DB_TRANSACTION_CONFLICT));
    assertThat(raced).filteredOn(outcome -> outcome == null).hasSizeLessThanOrEqualTo(1);
    assertThat(raced).contains(AnalyticsErrorCode.ANALYTICS_DB_TRANSACTION_CONFLICT);
    assertThat(lookupByName.find().isPresent()).isEqualTo(raced.contains(null));

    // The retries: the transaction conflict is retryable, and a retry sees the committed entity.
    List<@Nullable AnalyticsErrorCode> finalOutcomes = new ArrayList<>();
    for (AnalyticsErrorCode outcome : raced) {
      finalOutcomes.add(outcome == null ? null : retry(create));
    }
    assertThat(finalOutcomes).containsExactlyInAnyOrder(null, alreadyExists);
    assertThat(lookupByName.find()).isPresent();
  }

  private static @Nullable AnalyticsErrorCode retry(Attempt create) {
    @Nullable AnalyticsErrorCode outcome = AnalyticsErrorCode.ANALYTICS_DB_TRANSACTION_CONFLICT;
    for (int i = 0;
        i < MAX_RETRIES && outcome == AnalyticsErrorCode.ANALYTICS_DB_TRANSACTION_CONFLICT;
        i++) {
      outcome = run(create);
    }
    return outcome;
  }

  /** Runs both attempts on their own threads, holding each one just before its commit. */
  private static List<@Nullable AnalyticsErrorCode> race(Attempt first, Attempt second)
      throws Exception {
    CyclicBarrier barrier = new CyclicBarrier(2);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      List<Future<@Nullable AnalyticsErrorCode>> futures = new ArrayList<>();
      for (Attempt attempt : List.of(first, second)) {
        futures.add(
            executor.submit(
                () -> {
                  BEFORE_COMMIT.set(barrier);
                  try {
                    return run(attempt);
                  } finally {
                    BEFORE_COMMIT.remove();
                  }
                }));
      }
      List<@Nullable AnalyticsErrorCode> outcomes = new ArrayList<>();
      for (Future<@Nullable AnalyticsErrorCode> future : futures) {
        try {
          outcomes.add(future.get(60, TimeUnit.SECONDS));
        } catch (ExecutionException e) {
          throw new AssertionError("Attempt failed with an unexpected exception", e.getCause());
        }
      }
      return outcomes;
    } finally {
      executor.shutdownNow();
    }
  }

  /** Returns null if the attempt succeeded, or the error code it failed with. */
  private static @Nullable AnalyticsErrorCode run(Attempt attempt) {
    try {
      attempt.run();
      return null;
    } catch (AnalyticsException e) {
      return e.getErrorCode();
    }
  }

  @FunctionalInterface
  private interface Attempt {
    void run();
  }

  @FunctionalInterface
  private interface Lookup {
    Optional<?> find();
  }
}
