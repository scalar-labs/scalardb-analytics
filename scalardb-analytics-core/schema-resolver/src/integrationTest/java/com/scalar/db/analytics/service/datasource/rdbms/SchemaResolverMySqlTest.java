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
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchemaResolverMySqlTest extends RdbmsSchemaResolverWithDefaultDatabaseTestBase {
  private static final String INIT_SCRIPT = "sql/init_mysql.sql";

  @Container
  @SuppressWarnings("resource")
  private static final MariaDbMySqlContainer container =
      new MariaDbMySqlContainer()
          .withDatabaseName("testdb")
          // This is required because .withInitScript() runs scripts by non-root user, who cannot
          // create new databases.
          .withClasspathResourceMapping(
              INIT_SCRIPT, "/docker-entrypoint-initdb.d/init.sql", BindMode.READ_ONLY);

  private static final class MariaDbMySqlContainer extends MySQLContainer<MariaDbMySqlContainer> {
    private MariaDbMySqlContainer() {
      super(TestImages.MYSQL_80);
    }

    @Override
    public String getDriverClassName() {
      return "org.mariadb.jdbc.Driver";
    }

    @Override
    public String getJdbcUrl() {
      String url = super.getJdbcUrl();
      return url + (url.contains("?") ? "&" : "?") + "permitMysqlScheme=true";
    }
  }

  @BeforeAll
  void beforeAll() {
    super.beforeAllCommon();
  }

  @Override
  protected RdbmsSchemaResolver getSchemaResolver(RdbmsSchemaResolverOption option) {
    Properties properties = new Properties();
    properties.put("user", container.getUsername());
    properties.put("password", container.getPassword());
    return SchemaResolverMySql.open(
        DUMMY_SOURCE_ID, container.getJdbcUrl(), properties, option, null);
  }

  @Override
  protected ImmutableSet<List<String>> getAllNamespaces() {
    return ImmutableSet.of(Lists.newArrayList("testdb"), Lists.newArrayList("testdb2"));
  }

  @Override
  protected int getNamespaceLevel() {
    return 1;
  }

  @Override
  protected ImmutableSet<String> getAllTable() {
    return ImmutableSet.of("supported_types", "unsupported_types", "some_table");
  }

  @Override
  protected List<TestTable> getSupportedTypesTables() {
    return List.of(TestTable.of("testdb", "supported_types"));
  }

  @Override
  protected List<TypeMapping> getSupportedTypeMappings() {
    return Lists.newArrayList(
        new TypeMapping("bit_col", DataType.Boolean.INSTANCE),
        new TypeMapping("bit1_col", DataType.Boolean.INSTANCE),
        new TypeMapping("bit2_col", DataType.Blob.INSTANCE),
        new TypeMapping("tinyint_col", DataType.SmallInt.INSTANCE),
        new TypeMapping("tinyint1_col", DataType.Boolean.INSTANCE),
        new TypeMapping("boolean_col", DataType.Boolean.INSTANCE),
        new TypeMapping("smallint_col", DataType.SmallInt.INSTANCE),
        new TypeMapping("smallint_unsigned_col", DataType.Int.INSTANCE),
        new TypeMapping("mediumint_col", DataType.Int.INSTANCE),
        new TypeMapping("mediumint_unsigned_col", DataType.Int.INSTANCE),
        new TypeMapping("int_col", DataType.Int.INSTANCE),
        new TypeMapping("int_unsigned_col", DataType.BigInt.INSTANCE),
        new TypeMapping("bigint_col", DataType.BigInt.INSTANCE),
        new TypeMapping("float_col", DataType.Float.INSTANCE),
        new TypeMapping("double_col", DataType.Double.INSTANCE),
        new TypeMapping("real_col", DataType.Double.INSTANCE),
        new TypeMapping("char_col", DataType.Text.INSTANCE),
        new TypeMapping("varchar_col", DataType.Text.INSTANCE),
        new TypeMapping("tinytext_col", DataType.Text.INSTANCE),
        new TypeMapping("text_col", DataType.Text.INSTANCE),
        new TypeMapping("mediumtext_col", DataType.Text.INSTANCE),
        new TypeMapping("longtext_col", DataType.Text.INSTANCE),
        new TypeMapping("binary_col", DataType.Blob.INSTANCE),
        new TypeMapping("varbinary_col", DataType.Blob.INSTANCE),
        new TypeMapping("tinyblob_col", DataType.Blob.INSTANCE),
        new TypeMapping("blob_col", DataType.Blob.INSTANCE),
        new TypeMapping("mediumblob_col", DataType.Blob.INSTANCE),
        new TypeMapping("longblob_col", DataType.Blob.INSTANCE),
        new TypeMapping("date_col", DataType.Date.INSTANCE),
        new TypeMapping("time_col", DataType.Time.INSTANCE),
        new TypeMapping("datetime_col", DataType.Timestamp.INSTANCE),
        new TypeMapping("timestamp_col", DataType.TimestampTZ.INSTANCE));
  }

  @Override
  protected List<NullabilityMapping> getNullabilityMappings() {
    return Lists.newArrayList(
        new NullabilityMapping("int_col", true), new NullabilityMapping("not_null_col", false));
  }

  @Override
  protected List<TestTable> getUnsupportedTypesTables() {
    return List.of(TestTable.of("testdb", "unsupported_types"));
  }

  @Override
  protected RdbmsSchemaResolver getSchemaResolverWithDefaultDatabase() {
    Properties properties = new Properties();
    properties.put("user", container.getUsername());
    properties.put("password", container.getPassword());
    return SchemaResolverMySql.open(
        DUMMY_SOURCE_ID,
        container.getJdbcUrl(),
        properties,
        RdbmsSchemaResolverOption.DEFAULT,
        "testdb");
  }

  @Override
  protected ImmutableSet<List<String>> getAllNamespacesOfDefaultDatabase() {
    return ImmutableSet.of(List.of("testdb"));
  }

  @Override
  protected ImmutableSet<String> getAllTablesOfDefaultDatabase() {
    return ImmutableSet.of("supported_types", "unsupported_types");
  }
}
