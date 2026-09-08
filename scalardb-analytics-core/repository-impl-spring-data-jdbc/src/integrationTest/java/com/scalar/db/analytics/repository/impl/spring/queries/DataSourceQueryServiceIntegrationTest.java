/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.queries;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DataSourceQueryServiceIntegrationTest extends QueryServiceIntegrationTestBase {

  @Test
  void findDetailByName() {
    Optional<DataSource> detail = dataSourceQueryService.findByName(ctx, TEST_CATALOG, "ds1");

    assertThat(detail).isPresent();
    assertThat(detail.get().getName()).isEqualTo("ds1");
    assertThat(detail.get().getProvider()).isInstanceOf(PostgreSql.class);
  }

  @Test
  void findDetailByNameWithNonExistentDataSource() {
    Optional<DataSource> detail =
        dataSourceQueryService.findByName(ctx, TEST_CATALOG, "non-existent");

    assertThat(detail).isEmpty();
  }

  @Test
  void findDetailByNameWithNonExistentCatalog() {
    Optional<DataSource> detail =
        dataSourceQueryService.findByName(ctx, "non-existent-catalog", "ds1");

    assertThat(detail).isEmpty();
  }

  @Test
  void listByCatalogName() {
    List<DataSource> results = dataSourceQueryService.listByCatalogName(ctx, TEST_CATALOG);

    assertThat(results).hasSize(2);
    assertThat(results).extracting(ds -> ds.getName()).containsExactlyInAnyOrder("ds1", "ds2");
  }

  @Test
  void listByCatalogNameWithEmptyCatalog() {
    catalogRepository.create(ctx, Catalog.create("empty-catalog"));

    List<DataSource> results = dataSourceQueryService.listByCatalogName(ctx, "empty-catalog");

    assertThat(results).isEmpty();
  }

  @Test
  void listByCatalogNameWithNonExistentCatalog() {
    List<DataSource> results =
        dataSourceQueryService.listByCatalogName(ctx, "non-existent-catalog");

    assertThat(results).isEmpty();
  }
}
