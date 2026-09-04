/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.queries;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.DataSourceNamespace;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DataSourceNamespaceQueryServiceIntegrationTest extends QueryServiceIntegrationTestBase {

  @Test
  void listByCatalogName() {
    List<DataSourceNamespace> results =
        dataSourceNamespaceQueryService.listByCatalogName(ctx, TEST_CATALOG);

    assertThat(results).hasSize(3);

    List<DataSourceNamespace> ds1Results =
        results.stream().filter(dsn -> dsn.getDataSource().getName().equals("ds1")).toList();
    assertThat(ds1Results).hasSize(2);

    List<DataSourceNamespace> ds2Results =
        results.stream().filter(dsn -> dsn.getDataSource().getName().equals("ds2")).toList();
    assertThat(ds2Results).hasSize(1);

    assertThat(results.get(0).getNamespace().getNames()).isNotEmpty();
  }

  @Test
  void listByCatalogNameWithEmptyCatalog() {
    catalogRepository.create(ctx, Catalog.create("empty-catalog"));

    List<DataSourceNamespace> results =
        dataSourceNamespaceQueryService.listByCatalogName(ctx, "empty-catalog");

    assertThat(results).isEmpty();
  }

  @Test
  void listByDataSourceId() {
    UUID ds1Id =
        dataSourceQueryService.listByCatalogName(ctx, TEST_CATALOG).stream()
            .filter(ds -> ds.getName().equals("ds1"))
            .map(ds -> ds.getId())
            .findFirst()
            .orElseThrow();

    List<DataSourceNamespace> results =
        dataSourceNamespaceQueryService.listByDataSourceId(ctx, ds1Id);

    assertThat(results).hasSize(2);
    assertThat(results)
        .extracting(dsn -> dsn.getNamespace().getNames())
        .containsExactlyInAnyOrder(
            Arrays.asList("database1", "schema1"), Arrays.asList("database1", "schema2"));
  }

  @Test
  void listByDataSourceIdWithEmptyDataSource() {
    DataSource ds3 =
        new DataSource(
            UUID.randomUUID(),
            catalogId,
            "ds3",
            PostgreSql.builder()
                .host("host3")
                .port(5432)
                .username("user")
                .password("pass")
                .database("db3")
                .build());
    dataSourceRepository.create(ctx, ds3);

    List<DataSourceNamespace> results =
        dataSourceNamespaceQueryService.listByDataSourceId(ctx, ds3.getId());

    assertThat(results).isEmpty();
  }

  @Test
  void listByDataSourceIdWithNonExistentDataSource() {
    List<DataSourceNamespace> results =
        dataSourceNamespaceQueryService.listByDataSourceId(ctx, UUID.randomUUID());

    assertThat(results).isEmpty();
  }

  @Test
  void findByCatalogAndDataSourceAndNamesShouldReturnNamespace() {
    Optional<DataSourceNamespace> result =
        dataSourceNamespaceQueryService.findByCatalogAndDataSourceAndNames(
            ctx, TEST_CATALOG, "ds1", Arrays.asList("database1", "schema1"));

    assertThat(result).isPresent();
    assertThat(result.get().getNamespace().getNames())
        .isEqualTo(Arrays.asList("database1", "schema1"));
    assertThat(result.get().getDataSource().getName()).isEqualTo("ds1");
  }

  @Test
  void findByCatalogAndDataSourceAndNamesShouldReturnEmptyWhenNamespaceMissing() {
    Optional<DataSourceNamespace> result =
        dataSourceNamespaceQueryService.findByCatalogAndDataSourceAndNames(
            ctx, TEST_CATALOG, "ds1", Arrays.asList("nonexistent", "schema"));

    assertThat(result).isEmpty();
  }

  @Test
  void findByCatalogAndDataSourceAndNamesShouldReturnEmptyWhenDataSourceMissing() {
    Optional<DataSourceNamespace> result =
        dataSourceNamespaceQueryService.findByCatalogAndDataSourceAndNames(
            ctx, TEST_CATALOG, "nonexistent-ds", Arrays.asList("database1", "schema1"));

    assertThat(result).isEmpty();
  }

  @Test
  void findByCatalogAndDataSourceAndNamesShouldReturnEmptyWhenCatalogMissing() {
    Optional<DataSourceNamespace> result =
        dataSourceNamespaceQueryService.findByCatalogAndDataSourceAndNames(
            ctx, "nonexistent-catalog", "ds1", Arrays.asList("database1", "schema1"));

    assertThat(result).isEmpty();
  }
}
