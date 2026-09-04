/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.api.testing.DatabricksEnv;
import com.scalar.db.analytics.api.testing.SnowflakeEnv;
import com.scalar.db.analytics.api.testing.TestTable;
import com.scalar.db.analytics.service.datasource.NullabilityMapping;
import com.scalar.db.analytics.service.datasource.TypeMapping;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(
    named = DatabricksEnv.ANALYTICS_INT_TEST_ENV,
    matches = "CI",
    disabledReason = "Setting the 'ANALYTICS_INT_TEST_ENV=CI' environment variable is required")
public class SchemaResolverSnowflakeTest extends RdbmsSchemaResolverWithDefaultDatabaseTestBase {
  private static final Path INIT_SCRIPT =
      Paths.get("src", "integrationTest", "resources", "sql", "init_snowflake.sql");
  private static final SnowflakeEnv snowflakeEnv = new SnowflakeEnv();
  private static final String database1 =
      "SCHEMA_RESOLVER_TEST_DATABASE1_" + snowflakeEnv.getDatabaseSuffix();
  private static final String database2 =
      "SCHEMA_RESOLVER_TEST_DATABASE2_" + snowflakeEnv.getDatabaseSuffix();
  private static final HikariDataSource dataSource = snowflakeEnv.getDataSource();

  @BeforeAll
  void beforeAll() throws SQLException, IOException {
    executeSqlScript(INIT_SCRIPT);

    super.beforeAllCommon();
  }

  @AfterAll
  void afterAll() throws SQLException {
    executeSql(
        "DROP DATABASE IF EXISTS " + database1 + " CASCADE",
        "DROP DATABASE IF EXISTS " + database2 + " CASCADE");
    dataSource.close();
  }

  private void executeSqlScript(Path scriptPath) throws SQLException, IOException {
    String script = Files.readString(scriptPath);
    script =
        script
            .replaceAll("schema_resolver_test_database1", database1)
            .replaceAll("schema_resolver_test_database2", database2);
    String[] sqls =
        Arrays.stream(script.split(";"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toArray(String[]::new);
    executeSql(sqls);
  }

  private void executeSql(String... sqls) throws SQLException {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      for (String sql : sqls) {
        try {
          statement.execute(sql);
        } catch (SQLException e) {
          throw new SQLException("Failed to execute statement SQL statement: " + sql, e);
        }
      }
    }
  }

  @Override
  protected RdbmsSchemaResolver getSchemaResolver(RdbmsSchemaResolverOption option) {
    return SchemaResolverSnowflake.open(DUMMY_SOURCE_ID, snowflakeEnv.getSnowflake(), option, null);
  }

  @Override
  protected RdbmsSchemaResolver getDefaultSchemaResolver() {
    // Resolve only the namespaces related to this test to avoid conflicting with other parallel
    // integration tests run
    RdbmsSchemaResolverOption resolverOption =
        RdbmsSchemaResolverOption.builder().namespacesToResolve(getAllNamespaces()).build();
    return SchemaResolverSnowflake.open(
        DUMMY_SOURCE_ID, snowflakeEnv.getSnowflake(), resolverOption, null);
  }

  @Override
  protected ImmutableSet<List<String>> getAllNamespaces() {
    return ImmutableSet.of(
        Lists.newArrayList(database1, "PUBLIC"),
        Lists.newArrayList(database1, "SCHEMA1"),
        Lists.newArrayList(database1, "SCHEMA2"),
        Lists.newArrayList(database2, "PUBLIC"),
        Lists.newArrayList(database2, "SCHEMA1"),
        Lists.newArrayList(database2, "SCHEMA2"));
  }

  @Override
  protected int getNamespaceLevel() {
    return 2;
  }

  @Override
  protected ImmutableSet<String> getAllTable() {
    return ImmutableSet.of(
        "SUPPORTED_TYPES",
        "UNSUPPORTED_TYPES",
        "SUPPORTED_TYPES_HYBRID",
        "UNSUPPORTED_TYPES_HYBRID",
        "SOME_TABLE");
  }

  @Override
  protected List<TestTable> getSupportedTypesTables() {
    return List.of(
        TestTable.of(database1, "SCHEMA1", "SUPPORTED_TYPES"),
        TestTable.of(database1, "SCHEMA1", "SUPPORTED_TYPES_HYBRID"));
  }

  @Override
  protected List<TypeMapping> getSupportedTypeMappings() {
    return Lists.newArrayList(
        new TypeMapping("DECIMAL_20_COL", DataType.Decimal.builder().precision(2).scale(0).build()),
        new TypeMapping("DECIMAL_30_COL", DataType.Decimal.builder().precision(3).scale(0).build()),
        new TypeMapping("DECIMAL_40_COL", DataType.Decimal.builder().precision(4).scale(0).build()),
        new TypeMapping("DECIMAL_50_COL", DataType.Decimal.builder().precision(5).scale(0).build()),
        new TypeMapping("DECIMAL_90_COL", DataType.Decimal.builder().precision(9).scale(0).build()),
        new TypeMapping(
            "DECIMAL_100_COL", DataType.Decimal.builder().precision(10).scale(0).build()),
        new TypeMapping(
            "DECIMAL_180_COL", DataType.Decimal.builder().precision(18).scale(0).build()),
        new TypeMapping(
            "DECIMAL_190_COL", DataType.Decimal.builder().precision(19).scale(0).build()),
        new TypeMapping(
            "DECIMAL_380_COL", DataType.Decimal.builder().precision(38).scale(0).build()),
        new TypeMapping("DECIMAL_21_COL", DataType.Decimal.builder().precision(2).scale(1).build()),
        new TypeMapping("NUMBER_COL", DataType.Decimal.builder().precision(36).scale(0).build()),
        new TypeMapping("NUMERIC_COL", DataType.Decimal.builder().precision(33).scale(0).build()),
        new TypeMapping("INT_COL", DataType.Decimal.builder().precision(38).scale(0).build()),
        new TypeMapping("INTEGER_COL", DataType.Decimal.builder().precision(38).scale(0).build()),
        new TypeMapping("BIGINT_COL", DataType.Decimal.builder().precision(38).scale(0).build()),
        new TypeMapping("SMALLINT_COL", DataType.Decimal.builder().precision(38).scale(0).build()),
        new TypeMapping("TINYINT_COL", DataType.Decimal.builder().precision(38).scale(0).build()),
        new TypeMapping("BYTEINT_COL", DataType.Decimal.builder().precision(38).scale(0).build()),
        new TypeMapping("FLOAT_COL", DataType.Double.INSTANCE),
        new TypeMapping("FLOAT4_COL", DataType.Double.INSTANCE),
        new TypeMapping("FLOAT8_COL", DataType.Double.INSTANCE),
        new TypeMapping("DOUBLE_COL", DataType.Double.INSTANCE),
        new TypeMapping("DOUBLE_PRECISION_COL", DataType.Double.INSTANCE),
        new TypeMapping("REAL_COL", DataType.Double.INSTANCE),
        new TypeMapping("VARCHAR_COL", DataType.Text.INSTANCE),
        new TypeMapping("STRING_COL", DataType.Text.INSTANCE),
        new TypeMapping("TEXT_COL", DataType.Text.INSTANCE),
        new TypeMapping("NVARCHAR_COL", DataType.Text.INSTANCE),
        new TypeMapping("NVARCHAR2_COL", DataType.Text.INSTANCE),
        new TypeMapping("CHAR_VARYING_COL", DataType.Text.INSTANCE),
        new TypeMapping("NCHAR_VARYING_COL", DataType.Text.INSTANCE),
        new TypeMapping("CHAR_COL", DataType.Text.INSTANCE),
        new TypeMapping("CHARACTER_COL", DataType.Text.INSTANCE),
        new TypeMapping("NCHAR_COL", DataType.Text.INSTANCE),
        new TypeMapping("BINARY_COL", DataType.Blob.INSTANCE),
        new TypeMapping("VARBINARY_COL", DataType.Blob.INSTANCE),
        new TypeMapping("BOOLEAN_COL", DataType.Boolean.INSTANCE),
        new TypeMapping("DATE_COL", DataType.Date.INSTANCE),
        new TypeMapping("TIME_COL", DataType.Time.INSTANCE),
        new TypeMapping("TIMESTAMP_NTZ_COL", DataType.Timestamp.INSTANCE),
        new TypeMapping("DATETIME_COL", DataType.Timestamp.INSTANCE),
        new TypeMapping("TIMESTAMP_LTZ_COL", DataType.TimestampTZ.INSTANCE),
        new TypeMapping("TIMESTAMP_TZ_COL", DataType.TimestampTZ.INSTANCE));
  }

  @Override
  protected List<NullabilityMapping> getNullabilityMappings() {
    return Lists.newArrayList(
        new NullabilityMapping("INT_COL", true), new NullabilityMapping("NOT_NULL_COL", false));
  }

  @Override
  protected List<TestTable> getUnsupportedTypesTables() {
    return List.of(
        TestTable.of(database1, "SCHEMA1", "UNSUPPORTED_TYPES"),
        TestTable.of(database1, "SCHEMA1", "UNSUPPORTED_TYPES_HYBRID"));
  }

  @Override
  protected RdbmsSchemaResolver getSchemaResolverWithDefaultDatabase() {
    return SchemaResolverSnowflake.open(
        DUMMY_SOURCE_ID,
        dataSource.getJdbcUrl(),
        dataSource.getDataSourceProperties(),
        RdbmsSchemaResolverOption.DEFAULT,
        database1);
  }

  @Override
  protected ImmutableSet<List<String>> getAllNamespacesOfDefaultDatabase() {
    return ImmutableSet.of(
        Lists.newArrayList(database1, "PUBLIC"),
        Lists.newArrayList(database1, "SCHEMA1"),
        Lists.newArrayList(database1, "SCHEMA2"));
  }

  @Override
  protected ImmutableSet<String> getAllTablesOfDefaultDatabase() {
    return ImmutableSet.of(
        "SUPPORTED_TYPES",
        "UNSUPPORTED_TYPES",
        "SUPPORTED_TYPES_HYBRID",
        "UNSUPPORTED_TYPES_HYBRID");
  }
}
