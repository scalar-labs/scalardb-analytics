/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.analytics.api.model.Column;
import com.scalar.db.analytics.api.model.TableDetail;
import com.scalar.db.analytics.api.testing.TestImages;
import com.scalar.db.analytics.service.datasource.InvalidSchemaMetadataException;
import com.scalar.db.analytics.service.datasource.ResolvedSchema;
import com.scalar.db.analytics.service.datasource.SchemaMetadataValidator.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Checks that the PostgreSQL schema resolver rejects names that contain prohibited characters as it
 * reads them, and accepts printable Unicode names. The objects live in their own container, so that
 * the shared fixtures of {@link SchemaResolverPostgreSqlTest} stay resolvable.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchemaResolverPostgreSqlMetadataValidationTest {
  private static final UUID DATA_SOURCE_ID =
      UUID.fromString("5c0b6f0e-6a4e-4f4a-9d35-6f1f3c1d2b7a");

  // Built from a code point: Error Prone rejects bidi controls written in source.
  private static final String RLO = new String(Character.toChars(0x202E));

  @Container
  @SuppressWarnings("resource")
  private static final PostgreSQLContainer<?> container =
      new PostgreSQLContainer<>(TestImages.POSTGRESQL_16);

  @BeforeAll
  void beforeAll() throws SQLException {
    try (Connection conn =
            DriverManager.getConnection(
                container.getJdbcUrl(), container.getUsername(), container.getPassword());
        Statement stmt = conn.createStatement()) {
      stmt.execute("CREATE SCHEMA \"esc\u001bns\"");
      stmt.execute("CREATE TABLE \"esc\u001bns\".t (id INT)");
      stmt.execute("CREATE SCHEMA lf");
      stmt.execute("CREATE TABLE lf.\"or\nders\" (id INT)");
      stmt.execute("CREATE SCHEMA cr");
      stmt.execute("CREATE TABLE cr.t (\"na\rme\" INT)");
      stmt.execute("CREATE SCHEMA bidi");
      stmt.execute("CREATE TABLE bidi.t (\"abc" + RLO + "def\" INT)");
      stmt.execute("CREATE SCHEMA ty");
      stmt.execute("CREATE TYPE ty.\"mood\nINFO\" AS ENUM ('ok')");
      stmt.execute("CREATE TABLE ty.t (c ty.\"mood\nINFO\")");
      stmt.execute("CREATE SCHEMA \"販売\"");
      stmt.execute("CREATE TABLE \"販売\".\"注文\" (\"顧客名\" TEXT)");
    }
  }

  @Test
  void escInNamespaceName_shouldBeRejected() {
    assertRejected("esc\u001bns", null, Field.NAMESPACE, "\"esc\\u001bns\"", "esc\\u001bns");
  }

  @Test
  void newlineInTableName_shouldBeRejected() {
    assertRejected("lf", null, Field.TABLE, "\"lf\".\"or\\nders\"", "or\\nders");
  }

  @Test
  void carriageReturnInColumnName_shouldBeRejected() {
    assertRejected("cr", "t", Field.COLUMN, "\"cr\".\"t\".\"na\\rme\"", "na\\rme");
  }

  @Test
  void bidiOverrideInColumnName_shouldBeRejected() {
    assertRejected("bidi", "t", Field.COLUMN, "\"bidi\".\"t\".\"abc\\u202edef\"", "abc\\u202edef");
  }

  @Test
  void newlineInColumnTypeName_shouldBeRejected() {
    try (RdbmsSchemaResolver resolver = open("ty", "t")) {
      assertThatThrownBy(resolver::resolveSchema)
          .isInstanceOfSatisfying(
              InvalidSchemaMetadataException.class,
              e -> {
                assertThat(e.getField()).isEqualTo(Field.COLUMN_TYPE);
                assertThat(e.getPath()).isEqualTo("\"ty\".\"t\".\"c\"");
                assertThat(e.getValue()).contains("mood\\nINFO").doesNotContain("\n");
              });
    }
  }

  @Test
  void japaneseNames_shouldBeResolved() {
    ResolvedSchema schema;
    try (RdbmsSchemaResolver resolver = open("販売", null)) {
      schema = resolver.resolveSchema();
    }
    assertThat(schema.getNamespaces())
        .singleElement()
        .satisfies(ns -> assertThat(ns.getNames()).containsExactly("販売"));
    assertThat(schema.getTables())
        .singleElement()
        .satisfies(
            (TableDetail t) -> {
              assertThat(t.getInfo().getName()).isEqualTo("注文");
              assertThat(t.getColumns()).map(Column::getName).containsExactly("顧客名");
            });
  }

  private void assertRejected(
      String namespace, @Nullable String table, Field field, String path, String value) {
    try (RdbmsSchemaResolver resolver = open(namespace, table)) {
      assertThatThrownBy(resolver::resolveSchema)
          .isInstanceOfSatisfying(
              InvalidSchemaMetadataException.class,
              e -> {
                assertThat(e.getField()).isEqualTo(field);
                assertThat(e.getPath()).isEqualTo(path);
                assertThat(e.getValue()).isEqualTo(value);
              });
    }
  }

  private RdbmsSchemaResolver open(String namespace, @Nullable String table) {
    RdbmsSchemaResolverOption.RdbmsSchemaResolverOptionBuilder option =
        RdbmsSchemaResolverOption.builder().namespaceToResolve(List.of(namespace));
    if (table != null) {
      option.tableToResolve(table);
    }
    Properties properties = new Properties();
    properties.put("user", container.getUsername());
    properties.put("password", container.getPassword());
    return SchemaResolverPostgreSql.open(
        DATA_SOURCE_ID, container.getJdbcUrl(), properties, option.build());
  }
}
