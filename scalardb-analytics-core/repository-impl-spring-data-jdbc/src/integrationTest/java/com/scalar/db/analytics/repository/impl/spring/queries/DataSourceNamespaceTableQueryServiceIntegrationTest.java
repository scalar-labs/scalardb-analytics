/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.queries;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.api.model.Column;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTable;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTableDetail;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DataSourceNamespaceTableQueryServiceIntegrationTest extends QueryServiceIntegrationTestBase {

  @Test
  void listByCatalogName() {
    List<DataSourceNamespaceTable> results =
        dataSourceNamespaceTableQueryService.listByCatalogName(ctx, TEST_CATALOG);

    assertThat(results).hasSize(3);
    assertThat(results)
        .extracting(dsnt -> dsnt.getTable().getInfo().getName())
        .containsExactlyInAnyOrder("users", "orders", "products");

    DataSourceNamespaceTable userTable =
        results.stream()
            .filter(dsnt -> dsnt.getTable().getInfo().getName().equals("users"))
            .findFirst()
            .orElseThrow();
    assertThat(userTable.getNamespace().getNames()).containsExactly("database1", "schema1");
    assertThat(userTable.getDataSource().getName()).isEqualTo("ds1");
  }

  @Test
  void findDetailById() {
    UUID tableId = findTableByName("users").getTable().getInfo().getId();

    Optional<DataSourceNamespaceTableDetail> detail =
        dataSourceNamespaceTableQueryService.findDetailById(ctx, tableId);

    assertThat(detail).isPresent();
    assertThat(detail.get().getTable().getInfo().getName()).isEqualTo("users");
    assertThat(detail.get().getTable().getColumns()).hasSize(3);
    assertThat(detail.get().getTable().getColumns())
        .extracting(Column::getName)
        .containsExactly("id", "name", "email");
  }

  @Test
  void findDetailByIdWithNonExistentTable() {
    Optional<DataSourceNamespaceTableDetail> detail =
        dataSourceNamespaceTableQueryService.findDetailById(ctx, UUID.randomUUID());

    assertThat(detail).isEmpty();
  }

  @Test
  void listByNamespaceNames() {
    List<DataSourceNamespaceTable> results =
        dataSourceNamespaceTableQueryService.listByNamespaceNames(
            ctx, TEST_CATALOG, "ds1", Arrays.asList("database1", "schema1"));

    assertThat(results).hasSize(2);
    assertThat(results)
        .extracting(dsnt -> dsnt.getTable().getInfo().getName())
        .containsExactlyInAnyOrder("users", "orders");
  }

  @Test
  void listByNamespaceNamesWithNonExistentNamespace() {
    List<DataSourceNamespaceTable> results =
        dataSourceNamespaceTableQueryService.listByNamespaceNames(
            ctx, TEST_CATALOG, "ds1", Arrays.asList("non", "existent"));

    assertThat(results).isEmpty();
  }

  @Test
  void findDetailByName() {
    Optional<DataSourceNamespaceTableDetail> detail =
        dataSourceNamespaceTableQueryService.findDetailByName(
            ctx, TEST_CATALOG, "ds1", Arrays.asList("database1", "schema1"), "users");

    assertThat(detail).isPresent();
    assertThat(detail.get().getTable().getInfo().getName()).isEqualTo("users");
    assertThat(detail.get().getTable().getColumns()).hasSize(3);
  }

  @Test
  void findDetailByNameWithNonExistentTable() {
    Optional<DataSourceNamespaceTableDetail> detail =
        dataSourceNamespaceTableQueryService.findDetailByName(
            ctx, TEST_CATALOG, "ds1", Arrays.asList("database1", "schema1"), "nonexistent");

    assertThat(detail).isEmpty();
  }

  @Test
  void listByNamespaceId() {
    UUID namespaceId =
        resolveNamespace("ds1", Arrays.asList("database1", "schema1")).getNamespace().getId();

    List<DataSourceNamespaceTable> results =
        dataSourceNamespaceTableQueryService.listByNamespaceId(ctx, namespaceId);

    assertThat(results).hasSize(2);
    assertThat(results)
        .extracting(dsnt -> dsnt.getTable().getInfo().getName())
        .containsExactlyInAnyOrder("users", "orders");
  }

  @Test
  void listByNamespaceIdWithEmptyNamespace() {
    UUID emptyNamespaceId = createNamespace(ds1Detail.getId(), Arrays.asList("empty", "namespace"));

    List<DataSourceNamespaceTable> results =
        dataSourceNamespaceTableQueryService.listByNamespaceId(ctx, emptyNamespaceId);

    assertThat(results).isEmpty();
  }

  @Test
  void listByNamespaceIdWithNonExistentNamespace() {
    List<DataSourceNamespaceTable> results =
        dataSourceNamespaceTableQueryService.listByNamespaceId(ctx, UUID.randomUUID());

    assertThat(results).isEmpty();
  }
}
