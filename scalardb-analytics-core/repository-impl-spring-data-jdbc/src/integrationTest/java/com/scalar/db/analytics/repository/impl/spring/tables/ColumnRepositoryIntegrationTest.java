/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.tables;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.api.model.Column;
import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.api.model.TableDetail;
import com.scalar.db.analytics.api.model.TableInfo;
import com.scalar.db.analytics.repository.impl.spring.support.AbstractScalarDbIntegrationTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ColumnRepositoryIntegrationTest extends AbstractScalarDbIntegrationTest {

  private UUID catalogId;
  private UUID dataSourceId;
  private UUID namespaceId;

  @BeforeEach
  void setupSchema() {
    catalogId = createCatalog("column-catalog");
    dataSourceId = createPostgresDataSource(catalogId, "column-ds");
    namespaceId = createNamespace(dataSourceId, List.of("db", "analytics"));
  }

  @Test
  void listByTableIdShouldReturnColumnsOrderedByOrdinalPosition() {
    UUID tableId = UUID.randomUUID();
    TableDetail detail =
        new TableDetail(
            new TableInfo(tableId, namespaceId, "metrics"),
            List.of(
                new Column(UUID.randomUUID(), tableId, "z_col", DataType.Text.INSTANCE, 3, true),
                new Column(UUID.randomUUID(), tableId, "a_col", DataType.Int.INSTANCE, 1, false),
                new Column(
                    UUID.randomUUID(), tableId, "m_col", DataType.Double.INSTANCE, 2, true)));
    tableRepository.create(ctx, detail);

    List<Column> columns = columnRepository.listByTableId(ctx, tableId);

    assertThat(columns).hasSize(3);
    assertThat(columns).extracting(Column::getName).containsExactly("a_col", "m_col", "z_col");
  }

  @Test
  void listByTableIdShouldPreserveNullabilityFlags() {
    UUID tableId = UUID.randomUUID();
    TableDetail detail =
        new TableDetail(
            new TableInfo(tableId, namespaceId, "users"),
            List.of(
                new Column(UUID.randomUUID(), tableId, "id", DataType.BigInt.INSTANCE, 1, false),
                new Column(
                    UUID.randomUUID(), tableId, "nickname", DataType.Text.INSTANCE, 2, true)));
    tableRepository.create(ctx, detail);

    List<Column> columns = columnRepository.listByTableId(ctx, tableId);

    assertThat(columns).hasSize(2);
    assertThat(columns).extracting(Column::isNullable).containsExactly(false, true);
  }

  @Test
  void listByTableIdShouldReturnEmptyWhenTableHasNoColumns() {
    UUID tableId = UUID.randomUUID();
    tableRepository.create(
        ctx, new TableDetail(new TableInfo(tableId, namespaceId, "empty"), List.of()));

    assertThat(columnRepository.listByTableId(ctx, tableId)).isEmpty();
  }

  @Test
  void listByTableIdShouldReturnEmptyForUnknownTable() {
    assertThat(columnRepository.listByTableId(ctx, UUID.randomUUID())).isEmpty();
  }
}
