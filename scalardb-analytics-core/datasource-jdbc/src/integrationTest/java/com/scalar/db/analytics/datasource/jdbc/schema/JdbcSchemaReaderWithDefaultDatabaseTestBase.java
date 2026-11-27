/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.datasource.jdbc.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.schema.ResolvedSchema;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// Test class for RDBMS schema reader that can configure a default database
public abstract class JdbcSchemaReaderWithDefaultDatabaseTestBase extends JdbcSchemaReaderTestBase {

  @SuppressWarnings({"NotNullFieldNotInitialized", "NullAway.Init"})
  private ResolvedSchema resolvedSchemaWithDefaultDatabase;

  protected abstract JdbcSchemaReader getSchemaResolverWithDefaultDatabase();

  protected abstract ImmutableSet<List<String>> getAllNamespacesOfDefaultDatabase();

  protected abstract ImmutableSet<String> getAllTablesOfDefaultDatabase();

  protected @Override void beforeAllCommon() {
    super.beforeAllCommon();
    try (JdbcSchemaReader schemaReader = getSchemaResolverWithDefaultDatabase()) {
      resolvedSchemaWithDefaultDatabase = schemaReader.read();
    }
  }

  @Nested
  class ResolveSchemaWithDefaultDatabase {
    @Nested
    class Namespaces {
      @Test
      @DisplayName("should have a particular level namespace")
      void shouldHaveParticularLevelNamespace() {
        assertThat(resolvedSchemaWithDefaultDatabase.getNamespaces())
            .map(ns -> ns.getNames().size())
            .allMatch(s -> s == getNamespaceLevel());
      }

      @Test
      @DisplayName("should resolve all namespaces of default database")
      void shouldResolveAllNamespaces() {
        assertThat(resolvedSchemaWithDefaultDatabase.getNamespaces())
            .map(Namespace::getNames)
            .containsExactlyInAnyOrderElementsOf(getAllNamespacesOfDefaultDatabase());
      }
    }

    @Nested
    class Tables {
      @Test
      @DisplayName("should resolve all tables of default database")
      void shouldResolveAllTables() {
        assertThat(resolvedSchemaWithDefaultDatabase.getTables())
            .map(t -> t.getInfo().getName())
            .containsExactlyInAnyOrderElementsOf(getAllTablesOfDefaultDatabase());
      }
    }
  }
}
