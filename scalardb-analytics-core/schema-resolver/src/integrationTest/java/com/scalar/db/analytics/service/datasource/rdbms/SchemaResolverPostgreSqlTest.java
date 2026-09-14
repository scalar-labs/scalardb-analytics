/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.api.testing.TestImages;
import com.scalar.db.analytics.api.testing.TestTable;
import com.scalar.db.analytics.service.datasource.NullabilityMapping;
import com.scalar.db.analytics.service.datasource.TypeMapping;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchemaResolverPostgreSqlTest extends RdbmsSchemaResolverTestBase {
  private static final String INIT_SCRIPT = "sql/init_postgresql.sql";

  @Container
  @SuppressWarnings("resource")
  private static final PostgreSQLContainer<?> container =
      new PostgreSQLContainer<>(TestImages.POSTGRESQL_16).withInitScript(INIT_SCRIPT);

  @BeforeAll
  void beforeAll() {
    super.beforeAllCommon();
  }

  @Override
  protected RdbmsSchemaResolver getSchemaResolver(RdbmsSchemaResolverOption option) {
    Properties properties = new Properties();
    properties.put("user", container.getUsername());
    properties.put("password", container.getPassword());
    return SchemaResolverPostgreSql.open(
        DUMMY_SOURCE_ID, container.getJdbcUrl(), properties, option);
  }

  @Override
  protected ImmutableSet<List<String>> getAllNamespaces() {
    return ImmutableSet.of(
        Lists.newArrayList("public"), Lists.newArrayList("test"), Lists.newArrayList("test2"));
  }

  @Override
  protected int getNamespaceLevel() {
    return 1;
  }

  @Override
  protected ImmutableSet<String> getAllTable() {
    return ImmutableSet.of("supported_types", "unsupported_types");
  }

  @Override
  protected List<TestTable> getSupportedTypesTables() {
    return List.of(TestTable.of("test", "supported_types"));
  }

  @Override
  protected List<TypeMapping> getSupportedTypeMappings() {
    return Lists.newArrayList(
        new TypeMapping("smallint_col", DataType.SmallInt.INSTANCE),
        new TypeMapping("integer_col", DataType.Int.INSTANCE),
        new TypeMapping("bigint_col", DataType.BigInt.INSTANCE),
        new TypeMapping("real_col", DataType.Float.INSTANCE),
        new TypeMapping("double_col", DataType.Double.INSTANCE),
        new TypeMapping("smallserial_col", DataType.SmallInt.INSTANCE),
        new TypeMapping("serial_col", DataType.Int.INSTANCE),
        new TypeMapping("bigserial_col", DataType.BigInt.INSTANCE),
        new TypeMapping("char_col", DataType.Text.INSTANCE),
        new TypeMapping("varchar_col", DataType.Text.INSTANCE),
        new TypeMapping("text_col", DataType.Text.INSTANCE),
        new TypeMapping("bpchar_col", DataType.Text.INSTANCE),
        new TypeMapping("boolean_col", DataType.Boolean.INSTANCE),
        new TypeMapping("bytea_col", DataType.Blob.INSTANCE),
        new TypeMapping("date_col", DataType.Date.INSTANCE),
        new TypeMapping("time_col", DataType.Time.INSTANCE),
        new TypeMapping("time_with_timezone_col", DataType.Time.INSTANCE),
        new TypeMapping("time_without_timezone_col", DataType.Time.INSTANCE),
        new TypeMapping("timestamp_col", DataType.Timestamp.INSTANCE),
        new TypeMapping("timestamp_with_timezone_col", DataType.TimestampTZ.INSTANCE),
        new TypeMapping("timestamp_without_timezone_col", DataType.Timestamp.INSTANCE));
  }

  @Override
  protected List<NullabilityMapping> getNullabilityMappings() {
    return Lists.newArrayList(
        new NullabilityMapping("integer_col", true), new NullabilityMapping("not_null_col", false));
  }

  @Override
  protected List<TestTable> getUnsupportedTypesTables() {
    return List.of(TestTable.of("test", "unsupported_types"));
  }
}
