/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.api.model.TableDetail;
import com.scalar.db.analytics.api.testing.TestImages;
import com.scalar.db.analytics.api.testing.TestTable;
import com.scalar.db.analytics.service.datasource.NullabilityMapping;
import com.scalar.db.analytics.service.datasource.TypeMapping;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchemaResolverOracleTest extends RdbmsSchemaResolverTestBase {
  private static final String INIT_SCRIPT = "sql/init_oracle.sql";

  @Container
  private static final OracleContainer container =
      new OracleContainer(TestImages.ORACLE_23)
          .withInitScript(INIT_SCRIPT)
          .withUsername("testuser")
          .withStartupTimeout(TestImages.ORACLE_STARTUP_TIMEOUT);

  @BeforeAll
  void beforeAll() {
    super.beforeAllCommon();
  }

  @Override
  protected RdbmsSchemaResolver getSchemaResolver(RdbmsSchemaResolverOption option) {
    Properties properties = new Properties();
    properties.put("user", container.getUsername());
    properties.put("password", container.getPassword());
    return SchemaResolverOracle.open(DUMMY_SOURCE_ID, container.getJdbcUrl(), properties, option);
  }

  @Override
  protected ImmutableSet<List<String>> getAllNamespaces() {
    return ImmutableSet.of(Lists.newArrayList("TESTUSER"));
  }

  @Override
  protected int getNamespaceLevel() {
    return 1;
  }

  @Override
  protected ImmutableSet<String> getAllTable() {
    return ImmutableSet.of("SUPPORTED_TYPES", "UNSUPPORTED_TYPES", "LONG_COL_TABLE");
  }

  @Override
  protected List<TestTable> getSupportedTypesTables() {
    return List.of(TestTable.of("TESTUSER", "SUPPORTED_TYPES"));
  }

  @Override
  protected List<TypeMapping> getSupportedTypeMappings() {
    return Lists.newArrayList(
        new TypeMapping("NUMBER_COL", DataType.Double.INSTANCE),
        new TypeMapping("NUMBER_WITHOUT_SCALE_COL", DataType.BigInt.INSTANCE),
        new TypeMapping("NUMBER_WITH_SCALE_COL", DataType.Double.INSTANCE),
        new TypeMapping("FLOAT_WITH_PRECISION_53_COL", DataType.Double.INSTANCE),
        new TypeMapping("BINARY_FLOAT_COL", DataType.Float.INSTANCE),
        new TypeMapping("BINARY_DOUBLE_COL", DataType.Double.INSTANCE),
        new TypeMapping("CHAR_COL", DataType.Text.INSTANCE),
        new TypeMapping("NCHAR_COL", DataType.Text.INSTANCE),
        new TypeMapping("VARCHAR2_COL", DataType.Text.INSTANCE),
        new TypeMapping("NVARCHAR2_COL", DataType.Text.INSTANCE),
        new TypeMapping("CLOB_COL", DataType.Text.INSTANCE),
        new TypeMapping("NCLOB_COL", DataType.Text.INSTANCE),
        new TypeMapping("BLOB_COL", DataType.Blob.INSTANCE),
        new TypeMapping("BOOLEAN_COL", DataType.Boolean.INSTANCE),
        new TypeMapping("DATE_COL", DataType.Date.INSTANCE),
        new TypeMapping("TIMESTAMP_COL", DataType.TimestampTZ.INSTANCE),
        new TypeMapping("TIMESTAMP_WITH_TIME_ZONE_COL", DataType.TimestampTZ.INSTANCE),
        new TypeMapping("TIMESTAMP_WITH_LOCAL_TIME_ZONE_COL", DataType.Timestamp.INSTANCE),
        new TypeMapping("RAW_COL", DataType.Blob.INSTANCE));
  }

  @Override
  protected List<NullabilityMapping> getNullabilityMappings() {
    return Lists.newArrayList(
        new NullabilityMapping("NUMBER_COL", true), new NullabilityMapping("NOT_NULL_COL", false));
  }

  @Override
  protected List<TestTable> getUnsupportedTypesTables() {
    return List.of(TestTable.of("TESTUSER", "UNSUPPORTED_TYPES"));
  }

  /**
   * Test cases for the table with the 'long' type. This is required because the 'long' type is only
   * available in the table with a single column.
   */
  @Nested
  class LongColTable {
    @Test
    void long_shouldBeIgnored() {
      TableDetail table = tables.get("LONG_COL_TABLE");
      assertThat(table).isNotNull();
      assertThat(table.getColumns()).isEmpty();
    }
  }
}
