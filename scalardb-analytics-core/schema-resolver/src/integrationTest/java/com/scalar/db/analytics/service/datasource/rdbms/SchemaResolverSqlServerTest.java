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
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchemaResolverSqlServerTest extends RdbmsSchemaResolverWithDefaultDatabaseTestBase {
  private static final String INIT_SCRIPT = "sql/init_sql_server.sql";

  @Container
  @SuppressWarnings("resource")
  private static final MSSQLServerContainer<?> container =
      new MSSQLServerContainer<>(TestImages.SQL_SERVER_2019)
          .withInitScript(INIT_SCRIPT)
          .acceptLicense();

  @BeforeAll
  void beforeAll() {
    super.beforeAllCommon();
  }

  @Override
  protected RdbmsSchemaResolver getSchemaResolver(RdbmsSchemaResolverOption option) {
    Properties properties = new Properties();
    properties.put("user", container.getUsername());
    properties.put("password", container.getPassword());
    return SchemaResolverSqlServer.open(
        DUMMY_SOURCE_ID, container.getJdbcUrl(), properties, option, null);
  }

  @Override
  protected ImmutableSet<List<String>> getAllNamespaces() {
    return ImmutableSet.of(
        Lists.newArrayList("testdb", "dbo"),
        Lists.newArrayList("testdb", "schema1"),
        Lists.newArrayList("testdb", "schema2"),
        Lists.newArrayList("testdb2", "dbo"),
        Lists.newArrayList("testdb2", "schema1"),
        Lists.newArrayList("testdb2", "schema2"));
  }

  @Override
  protected int getNamespaceLevel() {
    return 2;
  }

  @Override
  protected ImmutableSet<String> getAllTable() {
    return ImmutableSet.of("supported_types", "unsupported_types", "some_table");
  }

  @Override
  protected List<TestTable> getSupportedTypesTables() {
    return List.of(TestTable.of("testdb", "schema1", "supported_types"));
  }

  @Override
  protected List<TypeMapping> getSupportedTypeMappings() {
    return Lists.newArrayList(
        new TypeMapping("bit_col", DataType.Boolean.INSTANCE),
        new TypeMapping("tinyint_col", DataType.SmallInt.INSTANCE),
        new TypeMapping("smallint_col", DataType.SmallInt.INSTANCE),
        new TypeMapping("int_col", DataType.Int.INSTANCE),
        new TypeMapping("bigint_col", DataType.BigInt.INSTANCE),
        new TypeMapping("real_col", DataType.Float.INSTANCE),
        new TypeMapping("float_col", DataType.Double.INSTANCE),
        new TypeMapping("float24_col", DataType.Float.INSTANCE),
        new TypeMapping("float25_col", DataType.Double.INSTANCE),
        new TypeMapping("float53_col", DataType.Double.INSTANCE),
        new TypeMapping("binary_col", DataType.Blob.INSTANCE),
        new TypeMapping("varbinary_col", DataType.Blob.INSTANCE),
        new TypeMapping("char_col", DataType.Text.INSTANCE),
        new TypeMapping("varchar_col", DataType.Text.INSTANCE),
        new TypeMapping("nchar_col", DataType.Text.INSTANCE),
        new TypeMapping("nvarchar_col", DataType.Text.INSTANCE),
        new TypeMapping("ntext_col", DataType.Text.INSTANCE),
        new TypeMapping("text_col", DataType.Text.INSTANCE),
        new TypeMapping("date_col", DataType.Date.INSTANCE),
        new TypeMapping("time_col", DataType.Time.INSTANCE),
        new TypeMapping("datetime_col", DataType.Timestamp.INSTANCE),
        new TypeMapping("datetime2_col", DataType.Timestamp.INSTANCE),
        new TypeMapping("smalldatetime_col", DataType.Timestamp.INSTANCE),
        new TypeMapping("datetimeoffset_col", DataType.TimestampTZ.INSTANCE));
  }

  @Override
  protected List<NullabilityMapping> getNullabilityMappings() {
    return Lists.newArrayList(
        new NullabilityMapping("int_col", true), new NullabilityMapping("not_null_col", false));
  }

  @Override
  protected List<TestTable> getUnsupportedTypesTables() {
    return List.of(TestTable.of("testdb", "schema1", "unsupported_types"));
  }

  @Override
  protected RdbmsSchemaResolver getSchemaResolverWithDefaultDatabase() {
    Properties properties = new Properties();
    properties.put("user", container.getUsername());
    properties.put("password", container.getPassword());
    return SchemaResolverSqlServer.open(
        DUMMY_SOURCE_ID,
        container.getJdbcUrl(),
        properties,
        RdbmsSchemaResolverOption.DEFAULT,
        "testdb");
  }

  @Override
  protected ImmutableSet<List<String>> getAllNamespacesOfDefaultDatabase() {
    return ImmutableSet.of(
        Lists.newArrayList("testdb", "dbo"),
        Lists.newArrayList("testdb", "schema1"),
        Lists.newArrayList("testdb", "schema2"));
  }

  @Override
  protected ImmutableSet<String> getAllTablesOfDefaultDatabase() {
    return ImmutableSet.of("supported_types", "unsupported_types");
  }
}
