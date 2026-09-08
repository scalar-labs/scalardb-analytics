/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.tables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.Column;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTable;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTableDetail;
import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.api.model.Table;
import com.scalar.db.analytics.api.model.TableDetail;
import com.scalar.db.analytics.api.model.TableInfo;
import com.scalar.db.analytics.repository.impl.spring.support.AbstractScalarDbIntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TableRepositoryIntegrationTest extends AbstractScalarDbIntegrationTest {

  private UUID catalogId;
  private UUID dataSourceId;
  private UUID namespaceId;

  @BeforeEach
  void setupNamespace() {
    catalogId = createCatalog("table-catalog");
    dataSourceId = createPostgresDataSource(catalogId, "table-ds");
    namespaceId = createNamespace(dataSourceId, List.of("db", "schema"));
  }

  @Test
  void createAndFindById() {
    UUID tableId = UUID.randomUUID();
    TableInfo info = new TableInfo(tableId, namespaceId, "orders");

    tableRepository.create(ctx, new TableDetail(info, List.of()));

    Optional<Table> table = tableRepository.findById(ctx, tableId);
    assertThat(table).isPresent();
    assertThat(table.get().getInfo().getName()).isEqualTo("orders");
  }

  @Test
  void findByIdShouldReturnEmptyWhenMissing() {
    assertThat(tableRepository.findById(ctx, UUID.randomUUID())).isEmpty();
  }

  @Test
  void createWithColumnsShouldPersistColumns() {
    UUID tableId = createTableWithDefaultColumns(namespaceId, "customers");

    List<Column> columns = columnRepository.listByTableId(ctx, tableId);

    assertThat(columns).hasSize(3);
    assertThat(columns).extracting(Column::getName).containsExactly("id", "name", "email");
  }

  @Test
  void createWithAllDataTypesShouldPersistColumns() {
    UUID tableId = UUID.randomUUID();

    // Create a list containing instances of all data types.
    List<DataType> allDataTypes =
        List.of(
            DataType.Byte.INSTANCE,
            DataType.SmallInt.INSTANCE,
            DataType.Int.INSTANCE,
            DataType.BigInt.INSTANCE,
            DataType.Float.INSTANCE,
            DataType.Double.INSTANCE,
            DataType.Decimal.DEFAULT_INSTANCE,
            DataType.Text.INSTANCE,
            DataType.Blob.INSTANCE,
            DataType.Boolean.INSTANCE,
            DataType.Date.INSTANCE,
            DataType.Time.INSTANCE,
            DataType.Timestamp.INSTANCE,
            DataType.TimestampTZ.INSTANCE,
            DataType.Duration.INSTANCE,
            DataType.Interval.INSTANCE);

    List<Column> columns = new ArrayList<>();
    int ordinal = 1;
    for (DataType type : allDataTypes) {
      columns.add(
          new Column(
              UUID.randomUUID(),
              tableId,
              type.getKind().name().toLowerCase(),
              type,
              ordinal++,
              ordinal % 2 == 0));
    }
    tableRepository.create(
        ctx, new TableDetail(new TableInfo(tableId, namespaceId, "all_types"), columns));

    List<Column> stored = columnRepository.listByTableId(ctx, tableId);
    assertThat(stored).hasSize(allDataTypes.size());
    assertThat(stored)
        .extracting(Column::getName)
        .containsExactlyElementsOf(columns.stream().map(Column::getName).toList());
    assertThat(stored)
        .extracting(Column::getType)
        .containsExactlyElementsOf(columns.stream().map(Column::getType).toList());
  }

  @Test
  void createDuplicateShouldThrowException() {
    tableRepository.create(
        ctx,
        new TableDetail(new TableInfo(UUID.randomUUID(), namespaceId, "dup_table"), List.of()));

    TableDetail duplicate =
        new TableDetail(new TableInfo(UUID.randomUUID(), namespaceId, "dup_table"), List.of());

    assertThatThrownBy(() -> tableRepository.create(ctx, duplicate))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            ex ->
                assertThat(((AnalyticsException) ex).getErrorCode())
                    .isEqualTo(AnalyticsErrorCode.TABLE_ALREADY_EXISTS));
  }

  @Test
  void queryServiceShouldReturnTableDetail() {
    UUID tableId = createTableWithDefaultColumns(namespaceId, "orders");

    Optional<DataSourceNamespaceTableDetail> detail =
        dataSourceNamespaceTableQueryService.findDetailByName(
            ctx, "table-catalog", "table-ds", List.of("db", "schema"), "orders");

    assertThat(detail).isPresent();
    assertThat(detail.get().getTable().getInfo().getId()).isEqualTo(tableId);
    assertThat(detail.get().getTable().getColumns()).hasSize(3);
  }

  @Test
  void queryServiceShouldListTablesByNamespaceId() {
    createTableWithDefaultColumns(namespaceId, "orders");
    createTableWithDefaultColumns(namespaceId, "invoices");

    List<DataSourceNamespaceTable> tables =
        dataSourceNamespaceTableQueryService.listByNamespaceId(ctx, namespaceId);

    assertThat(tables).hasSize(2);
    assertThat(tables)
        .extracting(t -> t.getTable().getInfo().getName())
        .containsExactlyInAnyOrder("orders", "invoices");
  }

  @Test
  void listByNamespaceIdShouldReturnEmptyWhenNoTables() {
    List<Table> tables = tableRepository.listByNamespaceId(ctx, namespaceId);

    assertThat(tables).isEmpty();
  }

  @Test
  void findByNamespaceIdAndNameShouldReturnTable() {
    UUID tableId = createTableWithDefaultColumns(namespaceId, "reporting");

    Optional<Table> table = tableRepository.findByNamespaceIdAndName(ctx, namespaceId, "reporting");

    assertThat(table).isPresent();
    assertThat(table.get().getInfo().getId()).isEqualTo(tableId);
  }

  @Test
  void findByNamespaceIdAndNameShouldReturnEmptyWhenMissing() {
    assertThat(tableRepository.findByNamespaceIdAndName(ctx, namespaceId, "missing")).isEmpty();
  }

  @Test
  void deleteByIdShouldRemoveTable() {
    UUID tableId = createTableWithDefaultColumns(namespaceId, "delete-me");

    tableRepository.deleteById(ctx, tableId);

    assertThat(tableRepository.findById(ctx, tableId)).isEmpty();
    assertThat(columnRepository.listByTableId(ctx, tableId)).isEmpty();
  }

  @Test
  void deleteByIdShouldNotThrowWhenMissing() {
    assertThatCode(() -> tableRepository.deleteById(ctx, UUID.randomUUID()))
        .doesNotThrowAnyException();
  }
}
