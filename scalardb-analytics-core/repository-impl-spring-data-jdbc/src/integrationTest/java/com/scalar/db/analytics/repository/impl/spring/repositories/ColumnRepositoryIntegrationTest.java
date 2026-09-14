/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repositories;

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
  void setUpSchema() {
    catalogId = createCatalog("column-catalog");
    dataSourceId = createPostgresDataSource(catalogId, "column-ds");
    namespaceId = createNamespace(dataSourceId, List.of("db", "analytics"));
  }

  @Test
  void listByTableIdShouldRespectOrdinalOrder() {
    UUID tableId = UUID.randomUUID();
    tableRepository.create(
        ctx,
        new TableDetail(
            new TableInfo(tableId, namespaceId, "metrics"),
            List.of(
                new Column(UUID.randomUUID(), tableId, "z", DataType.Text.INSTANCE, 3, true),
                new Column(UUID.randomUUID(), tableId, "a", DataType.Int.INSTANCE, 1, false),
                new Column(UUID.randomUUID(), tableId, "m", DataType.Double.INSTANCE, 2, true))));

    List<Column> columns = columnRepository.listByTableId(ctx, tableId);

    assertThat(columns).extracting(Column::getName).containsExactly("a", "m", "z");
  }

  @Test
  void listByTableIdShouldPreserveNullability() {
    UUID tableId = UUID.randomUUID();
    tableRepository.create(
        ctx,
        new TableDetail(
            new TableInfo(tableId, namespaceId, "users"),
            List.of(
                new Column(UUID.randomUUID(), tableId, "id", DataType.BigInt.INSTANCE, 1, false),
                new Column(
                    UUID.randomUUID(), tableId, "nickname", DataType.Text.INSTANCE, 2, true))));

    List<Column> columns = columnRepository.listByTableId(ctx, tableId);

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
  void listByTableIdShouldReturnEmptyWhenTableMissing() {
    assertThat(columnRepository.listByTableId(ctx, UUID.randomUUID())).isEmpty();
  }
}
