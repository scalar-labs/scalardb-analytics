/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.queries;

import com.scalar.db.analytics.api.model.Column;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.DataSourceNamespace;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTable;
import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.api.model.TableDetail;
import com.scalar.db.analytics.api.model.TableInfo;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;
import com.scalar.db.analytics.repository.impl.spring.support.AbstractScalarDbIntegrationTest;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;

abstract class QueryServiceIntegrationTestBase extends AbstractScalarDbIntegrationTest {

  protected static final String TEST_CATALOG = "test-catalog";

  protected UUID catalogId;
  protected DataSource ds1Detail;
  protected DataSource ds2Detail;
  protected UUID namespace1Id;
  protected UUID namespace2Id;

  @BeforeEach
  void initializeQueryFixture() {
    seedTestData();
  }

  protected void seedTestData() {
    catalogId = createCatalog(TEST_CATALOG);
    seedDataSources();
    seedNamespaces();
    seedTables();
  }

  private void seedDataSources() {
    ds1Detail =
        new DataSource(
            UUID.randomUUID(),
            catalogId,
            "ds1",
            PostgreSql.builder()
                .host("host1")
                .port(5432)
                .username("user")
                .password("pass")
                .database("db1")
                .build());
    dataSourceRepository.create(ctx, ds1Detail);

    ds2Detail =
        new DataSource(
            UUID.randomUUID(),
            catalogId,
            "ds2",
            PostgreSql.builder()
                .host("host2")
                .port(5432)
                .username("user")
                .password("pass")
                .database("db2")
                .build());
    dataSourceRepository.create(ctx, ds2Detail);
  }

  private void seedNamespaces() {
    namespace1Id = createNamespace(ds1Detail.getId(), Arrays.asList("database1", "schema1"));
    namespace2Id = createNamespace(ds1Detail.getId(), Arrays.asList("database1", "schema2"));
    createNamespace(ds2Detail.getId(), Arrays.asList("database2", "schema1"));
  }

  private void seedTables() {
    UUID usersTableId = UUID.randomUUID();
    tableRepository.create(
        ctx,
        new TableDetail(
            new TableInfo(usersTableId, namespace1Id, "users"),
            Arrays.asList(
                new Column(UUID.randomUUID(), usersTableId, "id", DataType.Int.INSTANCE, 1, false),
                new Column(
                    UUID.randomUUID(), usersTableId, "name", DataType.Text.INSTANCE, 2, false),
                new Column(
                    UUID.randomUUID(), usersTableId, "email", DataType.Text.INSTANCE, 3, true))));

    UUID ordersTableId = UUID.randomUUID();
    tableRepository.create(
        ctx,
        new TableDetail(
            new TableInfo(ordersTableId, namespace1Id, "orders"),
            Arrays.asList(
                new Column(UUID.randomUUID(), ordersTableId, "id", DataType.Int.INSTANCE, 1, false),
                new Column(
                    UUID.randomUUID(), ordersTableId, "user_id", DataType.Int.INSTANCE, 2, false),
                new Column(
                    UUID.randomUUID(),
                    ordersTableId,
                    "total",
                    DataType.Decimal.DEFAULT_INSTANCE,
                    3,
                    false))));

    UUID productsTableId = UUID.randomUUID();
    tableRepository.create(
        ctx,
        new TableDetail(
            new TableInfo(productsTableId, namespace2Id, "products"),
            Arrays.asList(
                new Column(
                    UUID.randomUUID(), productsTableId, "id", DataType.Int.INSTANCE, 1, false),
                new Column(
                    UUID.randomUUID(),
                    productsTableId,
                    "name",
                    DataType.Text.INSTANCE,
                    2,
                    false))));
  }

  protected DataSourceNamespaceTable findTableByName(String tableName) {
    return dataSourceNamespaceTableQueryService.listByCatalogName(ctx, TEST_CATALOG).stream()
        .filter(dsnt -> dsnt.getTable().getInfo().getName().equals(tableName))
        .findFirst()
        .orElseThrow();
  }

  protected DataSourceNamespace resolveNamespace(
      String dataSourceName, List<String> namespaceNames) {
    return dataSourceNamespaceQueryService
        .findByCatalogAndDataSourceAndNames(ctx, TEST_CATALOG, dataSourceName, namespaceNames)
        .orElseThrow();
  }

  protected void addAdditionalTable(UUID namespaceId, String tableName) {
    createTableWithDefaultColumns(namespaceId, tableName);
  }
}
