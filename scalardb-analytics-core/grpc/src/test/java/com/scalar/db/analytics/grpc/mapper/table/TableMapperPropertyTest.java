/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.analytics.api.model.Column;
import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.api.model.Table;
import com.scalar.db.analytics.api.model.TableDetail;
import com.scalar.db.analytics.api.model.TableInfo;
import com.scalar.db.analytics.grpc.mapper.CommonArbitraries;
import com.scalar.db.analytics.grpc.mapper.datatype.DataTypeJsonMapper;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class TableMapperPropertyTest {

  private final TableMapper mapper = TableMapper.INSTANCE;

  @Property
  void tableInfo_shouldRoundTripCorrectly(
      @ForAll("uuid") UUID id, @ForAll("uuid") UUID namespaceId, @ForAll("tableName") String name) {
    // Create domain table info
    TableInfo domainInfo = new TableInfo(id, namespaceId, name);

    // Domain -> Proto -> Domain
    com.scalar.db.analytics.grpc.generated.table.v1.TableInfo proto = mapper.toProto(domainInfo);
    TableInfo roundTripped = mapper.toDomain(proto);

    assertThat(roundTripped).isNotNull();
    assertThat(roundTripped.getId()).isEqualTo(id);
    assertThat(roundTripped.getNamespaceId()).isEqualTo(namespaceId);
    assertThat(roundTripped.getName()).isEqualTo(name);
  }

  @Property
  void table_shouldRoundTripCorrectly(
      @ForAll("uuid") UUID id, @ForAll("uuid") UUID namespaceId, @ForAll("tableName") String name) {
    // Create domain table
    TableInfo info = new TableInfo(id, namespaceId, name);
    Table domainTable = new Table(info);

    // Domain -> Proto -> Domain
    com.scalar.db.analytics.grpc.generated.table.v1.Table proto = mapper.toProto(domainTable);
    Table roundTripped = mapper.toDomain(proto);

    assertThat(roundTripped).isNotNull();
    assertThat(roundTripped.getInfo()).isNotNull();
    assertThat(roundTripped.getInfo().getId()).isEqualTo(id);
    assertThat(roundTripped.getInfo().getNamespaceId()).isEqualTo(namespaceId);
    assertThat(roundTripped.getInfo().getName()).isEqualTo(name);
  }

  @Property
  void column_shouldRoundTripCorrectly(
      @ForAll("uuid") UUID id,
      @ForAll("uuid") UUID tableId,
      @ForAll("columnName") String name,
      @ForAll("dataType") DataType type,
      @ForAll int ordinalPosition,
      @ForAll boolean nullable) {
    // Create domain column
    Column domainColumn = new Column(id, tableId, name, type, ordinalPosition, nullable);

    // Domain -> Proto -> Domain
    com.scalar.db.analytics.grpc.generated.table.v1.Column proto = mapper.toProto(domainColumn);
    Column roundTripped = mapper.toDomain(proto);

    assertThat(roundTripped).isNotNull();
    assertThat(roundTripped.getId()).isEqualTo(id);
    assertThat(roundTripped.getTableId()).isEqualTo(tableId);
    assertThat(roundTripped.getName()).isEqualTo(name);
    assertThat(roundTripped.getType()).isEqualTo(type);
    assertThat(roundTripped.getOrdinalPosition()).isEqualTo(ordinalPosition);
    assertThat(roundTripped.isNullable()).isEqualTo(nullable);
  }

  @Property
  void tableDetail_shouldRoundTripCorrectly(
      @ForAll("uuid") UUID tableId,
      @ForAll("uuid") UUID namespaceId,
      @ForAll("tableName") String tableName,
      @ForAll("columns") List<Column> columns) {
    // Create domain table detail
    TableInfo info = new TableInfo(tableId, namespaceId, tableName);
    TableDetail domainDetail = new TableDetail(info, columns);

    // Domain -> Proto -> Domain
    com.scalar.db.analytics.grpc.generated.table.v1.TableDetail proto =
        mapper.toProto(domainDetail);
    TableDetail roundTripped = mapper.toDomain(proto);

    assertThat(roundTripped).isNotNull();
    assertThat(roundTripped.getInfo()).isNotNull();
    assertThat(roundTripped.getInfo().getId()).isEqualTo(tableId);
    assertThat(roundTripped.getColumns()).hasSize(columns.size());
  }

  @Property
  void column_withDecimalType_shouldRoundTripCorrectly(
      @ForAll("uuid") UUID id,
      @ForAll("uuid") UUID tableId,
      @ForAll("columnName") String name,
      @ForAll("precision") int precision,
      @ForAll("scale") int scale,
      @ForAll int ordinalPosition,
      @ForAll boolean nullable) {
    // Create domain column with DECIMAL type
    DataType.Decimal decimalType =
        DataType.Decimal.builder().precision(precision).scale(scale).build();
    Column domainColumn = new Column(id, tableId, name, decimalType, ordinalPosition, nullable);

    // Domain -> Proto -> Domain
    com.scalar.db.analytics.grpc.generated.table.v1.Column proto = mapper.toProto(domainColumn);
    Column roundTripped = mapper.toDomain(proto);

    assertThat(roundTripped).isNotNull();
    assertThat(roundTripped.getId()).isEqualTo(id);
    assertThat(roundTripped.getTableId()).isEqualTo(tableId);
    assertThat(roundTripped.getName()).isEqualTo(name);
    assertThat(roundTripped.getType()).isInstanceOf(DataType.Decimal.class);

    DataType.Decimal roundTrippedDecimal = (DataType.Decimal) roundTripped.getType();
    assertThat(roundTrippedDecimal.getPrecision()).isEqualTo(precision);
    assertThat(roundTrippedDecimal.getScale()).isEqualTo(scale);
    assertThat(roundTripped.getOrdinalPosition()).isEqualTo(ordinalPosition);
    assertThat(roundTripped.isNullable()).isEqualTo(nullable);

    // Verify proto contains JSON representation
    assertThat(proto.getType()).contains("\"kind\":\"DECIMAL\"");
    assertThat(proto.getType()).contains("\"precision\":" + precision);
    assertThat(proto.getType()).contains("\"scale\":" + scale);
  }

  @Example
  void toProto_withNullElementInColumns_shouldSkipNullElements() {
    // Given
    UUID tableId = UUID.randomUUID();
    UUID namespaceId = UUID.randomUUID();
    UUID columnId = UUID.randomUUID();
    TableInfo info = new TableInfo(tableId, namespaceId, "test_table");
    Column col1 = new Column(columnId, tableId, "col1", DataType.Int.INSTANCE, 1, false);
    List<Column> columnsWithNull = Arrays.asList(col1, null);
    TableDetail detail = new TableDetail(info, columnsWithNull);

    // When
    com.scalar.db.analytics.grpc.generated.table.v1.TableDetail proto = mapper.toProto(detail);

    // Then
    assertThat(proto).isNotNull();
    assertThat(proto.getColumnsList()).hasSize(1);
  }

  @Property
  void toDomain_tableInfo_withInvalidUuidForId_shouldThrowException(
      @ForAll("invalidUuidString") String invalidId,
      @ForAll("uuid") UUID namespaceId,
      @ForAll("tableName") String name) {
    // Given
    com.scalar.db.analytics.grpc.generated.table.v1.TableInfo proto =
        com.scalar.db.analytics.grpc.generated.table.v1.TableInfo.newBuilder()
            .setId(invalidId)
            .setNamespaceId(namespaceId.toString())
            .setName(name)
            .build();

    // When & Then
    assertThatThrownBy(() -> mapper.toDomain(proto)).isInstanceOf(IllegalArgumentException.class);
  }

  @Property
  void toDomain_tableInfo_withInvalidUuidForNamespaceId_shouldThrowException(
      @ForAll("uuid") UUID id,
      @ForAll("invalidUuidString") String invalidNamespaceId,
      @ForAll("tableName") String name) {
    // Given
    com.scalar.db.analytics.grpc.generated.table.v1.TableInfo proto =
        com.scalar.db.analytics.grpc.generated.table.v1.TableInfo.newBuilder()
            .setId(id.toString())
            .setNamespaceId(invalidNamespaceId)
            .setName(name)
            .build();

    // When & Then
    assertThatThrownBy(() -> mapper.toDomain(proto)).isInstanceOf(IllegalArgumentException.class);
  }

  @Property
  void toDomain_column_withInvalidUuidForId_shouldThrowException(
      @ForAll("invalidUuidString") String invalidId,
      @ForAll("uuid") UUID tableId,
      @ForAll("columnName") String name,
      @ForAll("dataType") DataType type,
      @ForAll int ordinalPosition,
      @ForAll boolean nullable) {
    // Given
    String typeJson = DataTypeJsonMapper.INSTANCE.toJson(type);
    com.scalar.db.analytics.grpc.generated.table.v1.Column proto =
        com.scalar.db.analytics.grpc.generated.table.v1.Column.newBuilder()
            .setId(invalidId)
            .setTableId(tableId.toString())
            .setName(name)
            .setType(typeJson)
            .setOrdinalPosition(ordinalPosition)
            .setNullable(nullable)
            .build();

    // When & Then
    assertThatThrownBy(() -> mapper.toDomain(proto)).isInstanceOf(IllegalArgumentException.class);
  }

  @Property
  void toDomain_column_withInvalidUuidForTableId_shouldThrowException(
      @ForAll("uuid") UUID id,
      @ForAll("invalidUuidString") String invalidTableId,
      @ForAll("columnName") String name,
      @ForAll("dataType") DataType type,
      @ForAll int ordinalPosition,
      @ForAll boolean nullable) {
    // Given
    String typeJson = DataTypeJsonMapper.INSTANCE.toJson(type);
    com.scalar.db.analytics.grpc.generated.table.v1.Column proto =
        com.scalar.db.analytics.grpc.generated.table.v1.Column.newBuilder()
            .setId(id.toString())
            .setTableId(invalidTableId)
            .setName(name)
            .setType(typeJson)
            .setOrdinalPosition(ordinalPosition)
            .setNullable(nullable)
            .build();

    // When & Then
    assertThatThrownBy(() -> mapper.toDomain(proto)).isInstanceOf(IllegalArgumentException.class);
  }

  @Provide
  Arbitrary<UUID> uuid() {
    return CommonArbitraries.uuid();
  }

  @Provide
  Arbitrary<String> tableName() {
    return CommonArbitraries.tableName();
  }

  @Provide
  Arbitrary<String> columnName() {
    return CommonArbitraries.columnName();
  }

  @Provide
  Arbitrary<String> invalidUuidString() {
    return CommonArbitraries.invalidUuidString();
  }

  @Provide
  Arbitrary<DataType> dataType() {
    return Arbitraries.of(
        DataType.Int.INSTANCE,
        DataType.BigInt.INSTANCE,
        DataType.Text.INSTANCE,
        DataType.Blob.INSTANCE,
        DataType.Decimal.builder().precision(10).scale(2).build());
  }

  @Provide
  Arbitrary<Integer> precision() {
    return Arbitraries.integers().between(1, 38);
  }

  @Provide
  Arbitrary<Integer> scale() {
    return Arbitraries.integers().between(0, 38);
  }

  @Provide
  Arbitrary<List<Column>> columns() {
    UUID tableId = UUID.randomUUID();
    return Arbitraries.of(
        Arrays.asList(
            new Column(UUID.randomUUID(), tableId, "col1", DataType.Int.INSTANCE, 1, false)),
        Arrays.asList(
            new Column(UUID.randomUUID(), tableId, "col1", DataType.Int.INSTANCE, 1, false),
            new Column(UUID.randomUUID(), tableId, "col2", DataType.Text.INSTANCE, 2, true)),
        Arrays.asList());
  }
}
