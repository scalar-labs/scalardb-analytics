/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.datasource.scalardb.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.io.Resources;
import com.scalar.db.analytics.api.model.Column;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.TableDetail;
import com.scalar.db.analytics.api.testing.TestImages;
import com.scalar.db.analytics.service.datasource.ResolvedSchema;
import com.scalar.db.schemaloader.SchemaLoader;
import com.scalar.db.schemaloader.SchemaLoaderException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ScalarDbSchemaResolverTest {
  private static final UUID STUB_DATASOURCE_ID =
      UUID.fromString("064d4592-0929-4d53-80e8-9f8ef6a8f0f4");

  private static final UUID STUB_DATASOURCE_ID_FOR_SINGLE_CRUD =
      UUID.fromString("40200aee-4ad7-4c8a-b8f6-97a4ff42a1c3");

  @Container
  private static final PostgreSQLContainer<?> container =
      new PostgreSQLContainer<>(TestImages.POSTGRESQL_16);

  @Container
  private static final PostgreSQLContainer<?> containerCrud =
      new PostgreSQLContainer<>(TestImages.POSTGRESQL_16);

  @SuppressWarnings("NotNullFieldNotInitialized")
  private ResolvedSchema resolvedSchema;

  @SuppressWarnings("NotNullFieldNotInitialized")
  private ResolvedSchema resolvedSchemaCrud;

  @BeforeAll
  void beforeAll() throws SchemaLoaderException, URISyntaxException {
    loadSchemaForConsensusCommit();
    loadSchemaForSingleCrudOperation();
  }

  private void loadSchemaForConsensusCommit() throws SchemaLoaderException, URISyntaxException {
    Path schemaFilePath = Paths.get(Resources.getResource("scalardb/schema.json").toURI());
    Properties properties = new Properties();
    properties.setProperty("scalar.db.storage", "jdbc");
    properties.setProperty("scalar.db.contact_points", container.getJdbcUrl());
    properties.setProperty("scalar.db.username", container.getUsername());
    properties.setProperty("scalar.db.password", container.getPassword());
    SchemaLoader.load(properties, schemaFilePath, Collections.emptyMap(), true, false);
    resolvedSchema = new ScalarDbSchemaResolver(STUB_DATASOURCE_ID, properties).resolveSchema();
  }

  private void loadSchemaForSingleCrudOperation() throws SchemaLoaderException, URISyntaxException {
    Path schemaFilePath = Paths.get(Resources.getResource("scalardb/schema_crud.json").toURI());
    Properties properties = new Properties();
    properties.setProperty("scalar.db.transaction_manager", "single-crud-operation");
    properties.setProperty("scalar.db.storage", "jdbc");
    properties.setProperty("scalar.db.contact_points", containerCrud.getJdbcUrl());
    properties.setProperty("scalar.db.username", containerCrud.getUsername());
    properties.setProperty("scalar.db.password", containerCrud.getPassword());
    SchemaLoader.load(properties, schemaFilePath, Collections.emptyMap(), true, false);
    resolvedSchemaCrud =
        new ScalarDbSchemaResolver(STUB_DATASOURCE_ID_FOR_SINGLE_CRUD, properties).resolveSchema();
  }

  @Nested
  class ConsensusCommit {
    @Nested
    class ResolvedNamespace {
      @Test
      void shouldHaveSingleLevelNamespace() {
        assertThat(resolvedSchema.getNamespaces())
            .extracting(Namespace::getNames)
            .allMatch(names -> names.size() == 1);
      }

      @Test
      void shouldHaveUserDefinedNamespaces() {
        assertThat(resolvedSchema.getNamespaces())
            .extracting(Namespace::getNames)
            .containsExactlyInAnyOrder(Collections.singletonList("sample_db"));
      }
    }

    @Nested
    class ResolvedTables {
      @Test
      void shouldHaveUserDefinedTransactionTables() {
        assertThat(resolvedSchema.getTables())
            .extracting(t -> t.getInfo().getName())
            .containsExactlyInAnyOrder("sample_table", "sample_table2");
      }

      @Test
      void shouldHaveUserDefinedColumns() {
        TableDetail table = SchemaUtil.getTablesMap(resolvedSchema).get("sample_table");
        assertThat(table).isNotNull();
        assertThat(table.getColumns())
            .extracting(Column::getName)
            .containsExactly(
                // Primary key columns
                "int_col",
                // Clustering key columns
                "bigint_col",
                // Other user-defined columns
                "boolean_col",
                "float_col",
                "double_col",
                "text_col",
                "blob_col");
      }
    }
  }

  @Nested
  class SingleCrudOperation {
    @Nested
    class ResolvedNamespace {
      @Test
      void shouldHaveSingleLevelNamespace() {
        assertThat(resolvedSchemaCrud.getNamespaces())
            .extracting(Namespace::getNames)
            .allMatch(names -> names.size() == 1);
      }

      @Test
      void shouldHaveUserDefinedNamespaces() {
        assertThat(resolvedSchemaCrud.getNamespaces())
            .extracting(Namespace::getNames)
            .containsExactlyInAnyOrder(Collections.singletonList("sample_db_crud"));
      }
    }

    @Nested
    class ResolvedTables {
      @Test
      void shouldHaveUserDefinedTransactionTables() {
        assertThat(resolvedSchemaCrud.getTables())
            .extracting(t -> t.getInfo().getName())
            .containsExactlyInAnyOrder("sample_table", "sample_table2");
      }

      @Test
      void shouldHaveUserDefinedColumns() {
        TableDetail table = SchemaUtil.getTablesMap(resolvedSchemaCrud).get("sample_table");
        assertThat(table).isNotNull();
        assertThat(table.getColumns())
            .extracting(Column::getName)
            .containsExactly(
                // Primary key columns
                "int_col",
                // Clustering key columns
                "bigint_col",
                // Other user-defined columns
                "boolean_col",
                "float_col",
                "double_col",
                "text_col",
                "blob_col");
      }
    }
  }
}
