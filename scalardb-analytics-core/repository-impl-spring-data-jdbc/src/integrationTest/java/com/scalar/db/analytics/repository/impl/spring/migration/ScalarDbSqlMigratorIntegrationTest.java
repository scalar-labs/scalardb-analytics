/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.repository.impl.spring.autoconfigure.ScalarDbSqlProperties;
import com.scalar.db.analytics.repository.impl.spring.constants.ScalarDbNamespaces;
import com.scalar.db.api.DistributedTransactionAdmin;
import com.scalar.db.api.TableMetadata;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.schemaloader.SchemaLoaderException;
import com.scalar.db.service.TransactionFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class ScalarDbSqlMigratorIntegrationTest {

  @Container
  private final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16.4")
          .withDatabaseName("analytics_migration_it")
          .withUsername("analytics")
          .withPassword("analytics");

  private static final String BASE_SCHEMA_RESOURCE = "classpath:scalardb/schema-base.json";
  private static final List<String> BASE_SCHEMA_TABLES = List.of("catalogs");

  private static final String UPDATED_SCHEMA_RESOURCE =
      "classpath:scalardb/schema-extra-table.json";
  private static final List<String> UPDATED_SCHEMA_TABLES =
      List.of("catalogs", "registry_extra_table");

  private Properties scalarDbConfig;
  private String namespace;
  private ScalarDbSqlProperties sqlProperties;

  @BeforeEach
  void setUp() {
    sqlProperties =
        new ScalarDbSqlProperties(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

    scalarDbConfig = sqlProperties.toProperties();
    namespace =
        scalarDbConfig.getProperty(
            "scalar.db.sql.default_namespace_name", ScalarDbNamespaces.NAMESPACE);
  }

  @Test
  void migrateShouldProvisionSchemaAndRemainIdempotent() throws Exception {
    migrator(BASE_SCHEMA_RESOURCE).migrate();
    assertTablesPresent(BASE_SCHEMA_TABLES);

    migrator(BASE_SCHEMA_RESOURCE).migrate();
    assertTablesPresent(BASE_SCHEMA_TABLES);
  }

  @Test
  void migrateShouldApplySchemaUpdates() throws Exception {
    migrator(BASE_SCHEMA_RESOURCE).migrate();
    assertTablesPresent(BASE_SCHEMA_TABLES);
    assertThat(fetchColumnNamesFromAdmin("catalogs")).doesNotContain("description");

    migrator(UPDATED_SCHEMA_RESOURCE).migrate();
    assertTablesPresent(UPDATED_SCHEMA_TABLES);
    assertThat(fetchColumnNamesFromAdmin("catalogs")).contains("description");
  }

  private void assertTablesPresent(List<String> expectedTables) throws SchemaLoaderException {
    try (DistributedTransactionAdmin admin =
        TransactionFactory.create(scalarDbConfig).getTransactionAdmin()) {
      assertThat(admin.namespaceExists(namespace)).isTrue();
      assertThat(admin.coordinatorTablesExist()).isTrue();
      List<String> actualTables = new ArrayList<>(admin.getNamespaceTableNames(namespace));
      assertThat(actualTables)
          .as("tables provisioned in namespace %s", namespace)
          .containsExactlyInAnyOrderElementsOf(expectedTables);
    } catch (ExecutionException e) {
      throw new SchemaLoaderException("Failed to verify schema state", e);
    }
  }

  private ScalarDbSqlMigrator migrator(String schemaResource) {
    return new ScalarDbSqlMigrator(
        sqlProperties, new PathMatchingResourcePatternResolver(), schemaResource);
  }

  private List<String> fetchColumnNamesFromAdmin(String tableName) throws SchemaLoaderException {
    try (DistributedTransactionAdmin admin =
        TransactionFactory.create(scalarDbConfig).getTransactionAdmin()) {
      TableMetadata metadata = admin.getTableMetadata(namespace, tableName);
      assertThat(metadata).as("Table metadata not found: %s.%s", namespace, tableName).isNotNull();
      return new ArrayList<>(metadata.getColumnNames());
    } catch (ExecutionException e) {
      throw new SchemaLoaderException("Failed to verify table metadata", e);
    }
  }
}
