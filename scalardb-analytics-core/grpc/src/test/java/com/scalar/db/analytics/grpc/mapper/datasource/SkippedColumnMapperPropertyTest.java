/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.api.model.SkippedColumn;
import com.scalar.db.analytics.grpc.generated.datasource.v1.SkippedColumnWarning;
import com.scalar.db.analytics.grpc.mapper.CommonArbitraries;
import java.util.Arrays;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class SkippedColumnMapperPropertyTest {

  private final SkippedColumnMapper mapper = SkippedColumnMapper.INSTANCE;

  @Property
  void skippedColumn_shouldRoundTripCorrectly(
      @ForAll("namespaceNames") List<String> namespaceNames,
      @ForAll("tableName") String tableName,
      @ForAll("columnName") String columnName,
      @ForAll("sourceTypeName") String sourceTypeName,
      @ForAll("reason") String reason) {
    SkippedColumn domain =
        new SkippedColumn(namespaceNames, tableName, columnName, sourceTypeName, reason);

    // Domain -> Proto -> Domain
    SkippedColumn roundTripped = mapper.toDomain(mapper.toProto(domain));

    assertThat(roundTripped).isEqualTo(domain);
  }

  @Example
  void toProto_shouldMapEveryField() {
    SkippedColumn domain =
        new SkippedColumn(
            Arrays.asList("db", "public"),
            "events",
            "payload",
            "json",
            "The JDBC type is not supported: OTHER");

    SkippedColumnWarning proto = mapper.toProto(domain);

    assertThat(proto.getNamespaceNamesList()).containsExactly("db", "public");
    assertThat(proto.getTableName()).isEqualTo("events");
    assertThat(proto.getColumnName()).isEqualTo("payload");
    assertThat(proto.getSourceTypeName()).isEqualTo("json");
    assertThat(proto.getReason()).isEqualTo("The JDBC type is not supported: OTHER");
  }

  @Provide
  Arbitrary<List<String>> namespaceNames() {
    return CommonArbitraries.namespaceName().list().ofMinSize(1).ofMaxSize(3);
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
  Arbitrary<String> sourceTypeName() {
    return Arbitraries.of("json", "uuid", "inet", "money", "interval", "xml", "geometry");
  }

  @Provide
  Arbitrary<String> reason() {
    // Protobuf strings must be valid UTF-8, so unpaired surrogates are left out.
    return Arbitraries.strings()
        .alpha()
        .numeric()
        .withChars(" :.,()_-")
        .ofMinLength(1)
        .ofMaxLength(200);
  }
}
