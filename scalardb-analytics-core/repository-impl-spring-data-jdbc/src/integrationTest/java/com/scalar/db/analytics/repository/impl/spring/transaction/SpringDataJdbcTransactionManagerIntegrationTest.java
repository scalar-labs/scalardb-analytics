/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.repository.impl.spring.support.AbstractScalarDbIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SpringDataJdbcTransactionManagerIntegrationTest extends AbstractScalarDbIntegrationTest {

  @Nested
  class WithTransactionTests {

    @Test
    void shouldExecuteFunctionAndReturnResult() {
      Catalog catalog = Catalog.create("tx-result");

      Catalog result =
          transactionManager.withTransaction(
              ctx -> {
                catalogRepository.create(ctx, catalog);
                return catalog;
              });

      assertThat(result).isNotNull();
      assertThat(result.getName()).isEqualTo(catalog.getName());
      assertThat(catalogRepository.findByName(transactionManager.single(), catalog.getName()))
          .isPresent();
    }

    @Test
    void shouldRollbackAllOperationsOnException() {
      Catalog catalog1 = Catalog.create("tx-rollback-1");
      Catalog catalog2 = Catalog.create("tx-rollback-2");

      assertThatThrownBy(
              () ->
                  transactionManager.withTransaction(
                      ctx -> {
                        catalogRepository.create(ctx, catalog1);
                        catalogRepository.create(ctx, catalog2);
                        throw new AnalyticsException(
                            AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED);
                      }))
          .isInstanceOf(AnalyticsException.class);

      assertThat(catalogRepository.findByName(transactionManager.single(), catalog1.getName()))
          .isEmpty();
      assertThat(catalogRepository.findByName(transactionManager.single(), catalog2.getName()))
          .isEmpty();
      assertThat(catalogRepository.list(transactionManager.single())).isEmpty();
    }

    @Test
    void shouldCommitAllOperationsOnSuccess() {
      Catalog catalog1 = Catalog.create("tx-commit-1");
      Catalog catalog2 = Catalog.create("tx-commit-2");

      String result =
          transactionManager.withTransaction(
              ctx -> {
                catalogRepository.create(ctx, catalog1);
                catalogRepository.create(ctx, catalog2);
                return "success";
              });

      assertThat(result).isEqualTo("success");
      assertThat(catalogRepository.findByName(transactionManager.single(), catalog1.getName()))
          .isPresent();
      assertThat(catalogRepository.findByName(transactionManager.single(), catalog2.getName()))
          .isPresent();
      assertThat(catalogRepository.list(transactionManager.single())).hasSize(2);
    }

    @Test
    void shouldIsolateTransactionFromOtherOperations() {
      Catalog catalogOutside = Catalog.create("tx-isolate-outside");
      Catalog catalogInside = Catalog.create("tx-isolate-inside");

      catalogRepository.create(transactionManager.single(), catalogOutside);

      assertThatThrownBy(
              () ->
                  transactionManager.withTransaction(
                      ctx -> {
                        catalogRepository.create(ctx, catalogInside);
                        throw new AnalyticsException(
                            AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED);
                      }))
          .isInstanceOf(AnalyticsException.class);

      assertThat(catalogRepository.list(transactionManager.single())).hasSize(1);
      assertThat(
              catalogRepository.findByName(transactionManager.single(), catalogOutside.getName()))
          .isPresent();
      assertThat(catalogRepository.findByName(transactionManager.single(), catalogInside.getName()))
          .isEmpty();
    }

    @Test
    void shouldHandleNestedRepositoryCalls() {
      Catalog parent = Catalog.create("tx-parent");
      Catalog child = Catalog.create("tx-child");

      List<Catalog> catalogs =
          transactionManager.withTransaction(
              ctx -> {
                catalogRepository.create(ctx, parent);
                catalogRepository.create(ctx, child);
                return List.of(parent, child);
              });

      assertThat(catalogs).hasSize(2);
      assertThat(catalogs)
          .extracting(Catalog::getName)
          .containsExactlyInAnyOrder(parent.getName(), child.getName());
      assertThat(catalogRepository.list(transactionManager.single())).hasSize(2);
    }
  }

  @Nested
  class SingleTests {

    @Test
    void shouldUseExistingTransactionBoundary() {
      Catalog catalog = Catalog.create("single-boundary");
      transactionManager.withTransaction(
          ctx -> {
            catalogRepository.create(ctx, catalog);
            return null;
          });
      assertThat(catalogRepository.findByName(transactionManager.single(), catalog.getName()))
          .isPresent();
    }

    @Test
    void multipleCallsShouldHaveSeparateTransactions() {
      Catalog catalog1 = Catalog.create("single-multi-1");
      Catalog catalog2 = Catalog.create("single-multi-2");

      catalogRepository.create(transactionManager.single(), catalog1);
      catalogRepository.create(transactionManager.single(), catalog2);

      assertThat(catalogRepository.list(transactionManager.single())).hasSize(2);
      assertThat(catalogRepository.findByName(transactionManager.single(), catalog1.getName()))
          .isPresent();
      assertThat(catalogRepository.findByName(transactionManager.single(), catalog2.getName()))
          .isPresent();
    }

    @Test
    void shouldCommitEachOperationImmediately() {
      Catalog catalog = Catalog.create("single-commit");
      catalogRepository.create(transactionManager.single(), catalog);
      assertThat(catalogRepository.findByName(transactionManager.single(), catalog.getName()))
          .isPresent();
    }

    @Test
    void shouldNotRollbackOtherOperationsOnFailure() {
      Catalog success = Catalog.create("single-success");
      Catalog failure = Catalog.create("single-failure");

      catalogRepository.create(transactionManager.single(), success);
      catalogRepository.create(transactionManager.single(), failure);

      Catalog duplicate = Catalog.create(failure.getName());
      assertThatThrownBy(() -> catalogRepository.create(transactionManager.single(), duplicate))
          .isInstanceOf(Exception.class);

      assertThat(catalogRepository.list(transactionManager.single())).hasSize(2);
      assertThat(catalogRepository.findByName(transactionManager.single(), success.getName()))
          .isPresent();
      assertThat(catalogRepository.findByName(transactionManager.single(), failure.getName()))
          .isPresent();
    }
  }
}
