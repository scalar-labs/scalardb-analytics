/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.api.request.schema.ColumnSchema;
import com.scalar.db.analytics.grpc.mapper.CommonArbitraries;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class ColumnSchemaMapperPropertyTest {

  private final ColumnSchemaMapper mapper = ColumnSchemaMapper.INSTANCE;

  @Property
  void columnSchema_shouldRoundTripCorrectly(
      @ForAll("columnName") String name,
      @ForAll("dataType") DataType type,
      @ForAll boolean nullable) {
    // Create domain column schema
    ColumnSchema domainSchema = new ColumnSchema(name, type, nullable);

    // Domain -> Proto -> Domain
    com.scalar.db.analytics.grpc.generated.datasource.v1.ColumnSchema proto =
        mapper.toProto(domainSchema);
    ColumnSchema roundTripped = mapper.toDomain(proto);

    assertThat(roundTripped).isNotNull();
    assertThat(roundTripped.getName()).isEqualTo(name);
    assertThat(roundTripped.getType()).isEqualTo(type);
    assertThat(roundTripped.isNullable()).isEqualTo(nullable);
  }

  @Property
  void columnSchema_withDecimalType_shouldRoundTripCorrectly(
      @ForAll("columnName") String name,
      @ForAll("precision") int precision,
      @ForAll("scale") int scale,
      @ForAll boolean nullable) {
    // Create domain column schema with DECIMAL type
    DataType.Decimal decimalType =
        DataType.Decimal.builder().precision(precision).scale(scale).build();
    ColumnSchema domainSchema = new ColumnSchema(name, decimalType, nullable);

    // Domain -> Proto -> Domain
    com.scalar.db.analytics.grpc.generated.datasource.v1.ColumnSchema proto =
        mapper.toProto(domainSchema);
    ColumnSchema roundTripped = mapper.toDomain(proto);

    assertThat(roundTripped).isNotNull();
    assertThat(roundTripped.getName()).isEqualTo(name);
    assertThat(roundTripped.isNullable()).isEqualTo(nullable);
    assertThat(roundTripped.getType()).isInstanceOf(DataType.Decimal.class);

    DataType.Decimal roundTrippedDecimal = (DataType.Decimal) roundTripped.getType();
    assertThat(roundTrippedDecimal.getPrecision()).isEqualTo(precision);
    assertThat(roundTrippedDecimal.getScale()).isEqualTo(scale);

    // Verify proto contains JSON representation with precision and scale
    assertThat(proto.getType()).contains("\"kind\":\"DECIMAL\"");
    assertThat(proto.getType()).contains("\"precision\":" + precision);
    assertThat(proto.getType()).contains("\"scale\":" + scale);
  }

  @Provide
  Arbitrary<String> columnName() {
    return CommonArbitraries.columnName();
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
}
