/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.scalar.db.analytics.api.model.Column;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.TableDetail;
import com.scalar.db.analytics.api.testing.TestTable;
import com.scalar.db.analytics.service.datasource.NullabilityMapping;
import com.scalar.db.analytics.service.datasource.ResolvedSchema;
import com.scalar.db.analytics.service.datasource.SchemaUtil;
import com.scalar.db.analytics.service.datasource.TypeMapping;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.junit.jupiter.Testcontainers;

@TestInstance(Lifecycle.PER_CLASS)
@Testcontainers
@ParameterizedClass
@MethodSource("getTestTargetTablesArgs")
abstract class RdbmsSchemaResolverTestBase {

  @Parameter(0)
  @SuppressWarnings({"NotNullFieldNotInitialized", "NullAway.Init"})
  TestTable supportedTypesTable;

  @Parameter(1)
  @SuppressWarnings({"NotNullFieldNotInitialized", "NullAway.Init"})
  TestTable unsupportedTypesTable;

  protected static final UUID DUMMY_SOURCE_ID =
      UUID.fromString("064d4592-0929-4d53-80e8-9f8ef6a8f0f4");

  @SuppressWarnings({"NotNullFieldNotInitialized", "NullAway.Init"})
  protected ResolvedSchema resolvedSchema;

  @SuppressWarnings({"NotNullFieldNotInitialized", "NullAway.Init"})
  protected Map<List<String>, Namespace> namespaces;

  @SuppressWarnings({"NotNullFieldNotInitialized", "NullAway.Init"})
  protected Map<String, TableDetail> tables;

  protected void beforeAllCommon() {
    try (RdbmsSchemaResolver schemaResolver = getDefaultSchemaResolver()) {
      resolvedSchema = schemaResolver.resolveSchema();
    }

    namespaces = SchemaUtil.getNamespacesMap(resolvedSchema);
    tables = SchemaUtil.getTablesMap(resolvedSchema);
  }

  protected abstract RdbmsSchemaResolver getSchemaResolver(RdbmsSchemaResolverOption option);

  protected RdbmsSchemaResolver getDefaultSchemaResolver() {
    return getSchemaResolver(RdbmsSchemaResolverOption.DEFAULT);
  }

  protected abstract ImmutableSet<List<String>> getAllNamespaces();

  protected abstract int getNamespaceLevel();

  protected abstract ImmutableSet<String> getAllTable();

  protected abstract List<TestTable> getSupportedTypesTables();

  protected abstract List<TypeMapping> getSupportedTypeMappings();

  protected abstract List<NullabilityMapping> getNullabilityMappings();

  protected abstract List<TestTable> getUnsupportedTypesTables();

  @SuppressWarnings("unused") // This method is used to provide arguments for parameterized tests.
  private Stream<Arguments> getTestTargetTablesArgs() {
    Preconditions.checkArgument(
        getSupportedTypesTables().size() == getUnsupportedTypesTables().size(),
        "The number of supported types tables must match the number of unsupported types tables.");

    List<Arguments> args = new ArrayList<>();
    for (int i = 0; i < getSupportedTypesTables().size(); i++) {
      args.add(Arguments.of(getSupportedTypesTables().get(i), getUnsupportedTypesTables().get(i)));
    }
    return args.stream();
  }

  @Nested
  class ResolveSchema {

    @Test
    @DisplayName(
        "when failOnUnsupportedDataType is true, should fail if unsupported data types exists")
    void whenFailOnUnsupportedDataTypeIsTrue_shouldThrowUnsupportedJdbcTypeException() {
      RdbmsSchemaResolverOption option =
          RdbmsSchemaResolverOption.builder()
              .namespaceToResolve(unsupportedTypesTable.getNamespace())
              .tableToResolve(unsupportedTypesTable.getTableName())
              .failOnUnsupportedDataType(true)
              .build();

      try (RdbmsSchemaResolver schemaResolver = getSchemaResolver(option)) {
        assertThatThrownBy(schemaResolver::resolveSchema)
            .isInstanceOf(UnsupportedJdbcTypeException.class);
      }
    }

    @Nested
    class Namespaces {
      @Test
      @DisplayName("should have a particular level namespace")
      void shouldHaveParticularLevelNamespace() {
        assertThat(resolvedSchema.getNamespaces())
            .map(ns -> ns.getNames().size())
            .allMatch(s -> s == getNamespaceLevel());
      }

      @Test
      @DisplayName("should resolve all namespaces")
      void shouldResolveAllNamespaces() {
        assertThat(resolvedSchema.getNamespaces())
            .map(Namespace::getNames)
            .containsExactlyInAnyOrderElementsOf(getAllNamespaces());
      }
    }

    @Nested
    class Tables {
      private final UUID namespaceId =
          Objects.requireNonNull(namespaces.get(supportedTypesTable.getNamespace())).getId();

      @Test
      @DisplayName("should resolve all tables")
      void shouldResolveAllTables() {
        assertThat(resolvedSchema.getTables())
            .map(t -> t.getInfo().getName())
            .containsExactlyInAnyOrderElementsOf(getAllTable());
      }

      @Nested
      class SupportedTypes {

        private final TableDetail table =
            Objects.requireNonNull(tables.get(supportedTypesTable.getTableName()));

        private final Map<String, Column> columns = SchemaUtil.getColumnsMap(table);

        @Test
        @DisplayName("should belong to the namespace")
        void shouldBelongToTargetNamespace() {
          assertThat(table.getInfo().getNamespaceId()).isEqualTo(namespaceId);
        }

        @TestFactory
        @DisplayName("should map supported JDBC types to data types correctly")
        Stream<DynamicNode> shouldMapSupportedJdbcTypeToDataTypeCorrectly() {
          return getSupportedTypeMappings().stream()
              .map(
                  m ->
                      dynamicTest(
                          m.getDisplayName(),
                          () -> {
                            Column column = columns.get(m.getColumnName());
                            assertThat(column).isNotNull();
                            assertThat(column.getType()).isEqualTo(m.getDataType());
                          }));
        }

        @TestFactory
        @DisplayName("should map nullability correctly")
        Stream<DynamicNode> shouldMapNullabilityCorrectly() {
          return getNullabilityMappings().stream()
              .map(
                  m ->
                      dynamicTest(
                          m.getDisplayName(),
                          () -> {
                            Column column = columns.get(m.getColumnName());
                            assertThat(column).isNotNull();
                            assertThat(column.isNullable()).isEqualTo(m.isNullable());
                          }));
        }
      }

      @Nested
      class UnsupportedTypes {

        private final TableDetail table =
            Objects.requireNonNull(tables.get(unsupportedTypesTable.getTableName()));

        private final Map<String, Column> columns = SchemaUtil.getColumnsMap(table);

        @Test
        @DisplayName("should belong to the namespace")
        void shouldBelongToTargetNamespace() {
          assertThat(table.getInfo().getNamespaceId()).isEqualTo(namespaceId);
        }

        @Test
        @DisplayName("should ignore unsupported JDBC types")
        void shouldBeIgnored() {
          Map<String, Column> columnsToCheck =
              columns.entrySet().stream()
                  .filter(e -> !e.getKey().equalsIgnoreCase("ignored_col"))
                  .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
          assertThat(columnsToCheck).isEmpty();
        }
      }
    }
  }
}
