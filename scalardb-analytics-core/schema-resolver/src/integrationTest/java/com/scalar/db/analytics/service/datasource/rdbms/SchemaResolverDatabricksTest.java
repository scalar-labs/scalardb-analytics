/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.api.testing.DatabricksEnv;
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
public class SchemaResolverDatabricksTest extends RdbmsSchemaResolverWithDefaultDatabaseTestBase {
  private static final Path INIT_SCRIPT =
      Paths.get("src", "integrationTest", "resources", "sql", "init_databricks.sql");
  private static final DatabricksEnv databricksEnv = new DatabricksEnv();
  private static final String catalog1 =
      "schema_resolver_test_catalog1_" + databricksEnv.getCatalogSuffix();
  private static final String catalog2 =
      "schema_resolver_test_catalog2_" + databricksEnv.getCatalogSuffix();
  private static final HikariDataSource dataSource = databricksEnv.getDataSource();
  private static final String schemaHive1 =
      "schema_resolver_test_schema_hive1_" + databricksEnv.getCatalogSuffix();
  private static final String schemaHive2 =
      "schema_resolver_test_schema_hive2_" + databricksEnv.getCatalogSuffix();

  @BeforeAll
  void beforeAll() throws SQLException, IOException {
    executeSqlScript(INIT_SCRIPT);

    super.beforeAllCommon();
  }

  @AfterAll
  void afterAll() throws SQLException {

    executeSql(
        "DROP CATALOG IF EXISTS " + catalog1 + " CASCADE",
        "DROP CATALOG IF EXISTS " + catalog2 + " CASCADE",
        "DROP SCHEMA IF EXISTS hive_metastore." + schemaHive1 + " CASCADE",
        "DROP SCHEMA IF EXISTS hive_metastore." + schemaHive2 + " CASCADE");
    dataSource.close();
  }

  private void executeSqlScript(Path scriptPath) throws SQLException, IOException {
    String script = Files.readString(scriptPath);
    script =
        script
            .replaceAll("schema_resolver_test_catalog1", catalog1)
            .replaceAll("schema_resolver_test_catalog2", catalog2)
            .replaceAll("schema_resolver_test_schema_hive1", schemaHive1)
            .replaceAll("schema_resolver_test_schema_hive2", schemaHive2);
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
        statement.execute(sql);
      }
    }
  }

  @Override
  protected RdbmsSchemaResolver getSchemaResolver(RdbmsSchemaResolverOption option) {
    return SchemaResolverDatabricks.open(
        DUMMY_SOURCE_ID, databricksEnv.getDatabricks(), option, null);
  }

  @Override
  protected RdbmsSchemaResolver getDefaultSchemaResolver() {
    // Resolve only the namespaces related to this test to avoid conflicting with other parallel
    // integration tests run
    RdbmsSchemaResolverOption resolverOption =
        RdbmsSchemaResolverOption.builder().namespacesToResolve(getAllNamespaces()).build();
    return SchemaResolverDatabricks.open(
        DUMMY_SOURCE_ID, databricksEnv.getDatabricks(), resolverOption, null);
  }

  @Override
  protected ImmutableSet<List<String>> getAllNamespaces() {
    return ImmutableSet.of(
        Lists.newArrayList("main", "default"),
        Lists.newArrayList(catalog1, "default"),
        Lists.newArrayList(catalog1, "schema1"),
        Lists.newArrayList(catalog1, "schema2"),
        Lists.newArrayList(catalog2, "default"),
        Lists.newArrayList(catalog2, "schema1"),
        Lists.newArrayList(catalog2, "schema2"),
        Lists.newArrayList("hive_metastore", schemaHive1),
        Lists.newArrayList("hive_metastore", schemaHive2));
  }

  @Override
  protected int getNamespaceLevel() {
    return 2;
  }

  @Override
  protected ImmutableSet<String> getAllTable() {
    return ImmutableSet.of(
        "supported_types",
        "unsupported_types",
        "some_table",
        "supported_types_hive",
        "unsupported_types_hive");
  }

  @Override
  protected List<TestTable> getSupportedTypesTables() {
    return List.of(
        TestTable.of(catalog1, "schema1", "supported_types"),
        TestTable.of("hive_metastore", schemaHive1, "supported_types_hive"));
  }

  @Override
  protected List<TypeMapping> getSupportedTypeMappings() {
    return Lists.newArrayList(
        new TypeMapping("tinyint_col", DataType.SmallInt.INSTANCE),
        new TypeMapping("smallint_col", DataType.SmallInt.INSTANCE),
        new TypeMapping("int_col", DataType.Int.INSTANCE),
        new TypeMapping("bigint_col", DataType.BigInt.INSTANCE),
        new TypeMapping("float_col", DataType.Float.INSTANCE),
        new TypeMapping("double_col", DataType.Double.INSTANCE),
        new TypeMapping("decimal_col", DataType.Decimal.builder().precision(10).scale(0).build()),
        new TypeMapping("decimal_20_col", DataType.Byte.INSTANCE),
        new TypeMapping("decimal_30_col", DataType.SmallInt.INSTANCE),
        new TypeMapping("decimal_40_col", DataType.SmallInt.INSTANCE),
        new TypeMapping("decimal_50_col", DataType.Int.INSTANCE),
        new TypeMapping("decimal_90_col", DataType.Int.INSTANCE),
        new TypeMapping("decimal_100_col", DataType.BigInt.INSTANCE),
        new TypeMapping("decimal_180_col", DataType.BigInt.INSTANCE),
        new TypeMapping(
            "decimal_190_col", DataType.Decimal.builder().precision(19).scale(0).build()),
        new TypeMapping("decimal_21_col", DataType.Decimal.builder().precision(2).scale(1).build()),
        new TypeMapping("string_col", DataType.Text.INSTANCE),
        new TypeMapping("binary_col", DataType.Blob.INSTANCE),
        new TypeMapping("boolean_col", DataType.Boolean.INSTANCE),
        new TypeMapping("date_col", DataType.Date.INSTANCE),
        new TypeMapping("timestamp_col", DataType.TimestampTZ.INSTANCE),
        new TypeMapping("timestamp_ntz_col", DataType.Timestamp.INSTANCE));
  }

  @Override
  protected List<NullabilityMapping> getNullabilityMappings() {
    return Lists.newArrayList(
        new NullabilityMapping("int_col", true), new NullabilityMapping("not_null_col", false));
  }

  @Override
  protected List<TestTable> getUnsupportedTypesTables() {
    return List.of(
        TestTable.of(catalog1, "schema1", "unsupported_types"),
        TestTable.of("hive_metastore", schemaHive1, "unsupported_types_hive"));
  }

  @Override
  protected RdbmsSchemaResolver getSchemaResolverWithDefaultDatabase() {
    return SchemaResolverDatabricks.open(
        DUMMY_SOURCE_ID,
        dataSource.getJdbcUrl(),
        dataSource.getDataSourceProperties(),
        RdbmsSchemaResolverOption.DEFAULT,
        catalog1);
  }

  @Override
  protected ImmutableSet<List<String>> getAllNamespacesOfDefaultDatabase() {
    return ImmutableSet.of(
        Lists.newArrayList(catalog1, "default"),
        Lists.newArrayList(catalog1, "schema1"),
        Lists.newArrayList(catalog1, "schema2"));
  }

  @Override
  protected ImmutableSet<String> getAllTablesOfDefaultDatabase() {
    return ImmutableSet.of("supported_types", "unsupported_types");
  }
}
