/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.support;

import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.api.model.Column;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.TableDetail;
import com.scalar.db.analytics.api.model.TableInfo;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;
import com.scalar.db.analytics.repository.impl.spring.SpringDataJdbcIntegrationTestConfiguration;
import com.scalar.db.analytics.repository.impl.spring.migration.ScalarDbSqlMigrator;
import com.scalar.db.analytics.repository.impl.spring.query.DataSourceNamespaceQueryServiceImpl;
import com.scalar.db.analytics.repository.impl.spring.query.DataSourceNamespaceTableQueryServiceImpl;
import com.scalar.db.analytics.repository.impl.spring.query.DataSourceQueryServiceImpl;
import com.scalar.db.analytics.repository.impl.spring.repository.CatalogRepositoryImpl;
import com.scalar.db.analytics.repository.impl.spring.repository.ColumnRepositoryImpl;
import com.scalar.db.analytics.repository.impl.spring.repository.DataSourceRepositoryImpl;
import com.scalar.db.analytics.repository.impl.spring.repository.NamespaceRepositoryImpl;
import com.scalar.db.analytics.repository.impl.spring.repository.TableRepositoryImpl;
import com.scalar.db.analytics.repository.impl.spring.transaction.SpringDataJdbcTransactionContext;
import com.scalar.db.analytics.repository.impl.spring.transaction.SpringDataJdbcTransactionManager;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(
    classes = SpringDataJdbcIntegrationTestConfiguration.class,
    properties = {"spring.main.allow-bean-definition-overriding=false"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractScalarDbIntegrationTest {

  @SuppressWarnings("resource")
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16.4")
          .withDatabaseName("analytics_it")
          .withUsername("analytics")
          .withPassword("analytics");

  static {
    POSTGRES.start();
    Runtime.getRuntime().addShutdownHook(new Thread(POSTGRES::stop));
  }

  @DynamicPropertySource
  static void configureTestDatabase(DynamicPropertyRegistry registry) {
    registry.add("scalar.db.analytics.server.db.contact-points", POSTGRES::getJdbcUrl);
    registry.add("scalar.db.analytics.server.db.username", POSTGRES::getUsername);
    registry.add("scalar.db.analytics.server.db.password", POSTGRES::getPassword);
    registry.add("scalar.db.analytics.sql.migration.enabled", () -> "true");
  }

  @Autowired protected JdbcTemplate jdbcTemplate;
  @Autowired protected SpringDataJdbcTransactionManager transactionManager;
  @Autowired protected ScalarDbSqlMigrator migrator;
  @Autowired protected CatalogRepositoryImpl catalogRepository;
  @Autowired protected DataSourceRepositoryImpl dataSourceRepository;
  @Autowired protected NamespaceRepositoryImpl namespaceRepository;
  @Autowired protected TableRepositoryImpl tableRepository;
  @Autowired protected ColumnRepositoryImpl columnRepository;
  @Autowired protected DataSourceQueryServiceImpl dataSourceQueryService;
  @Autowired protected DataSourceNamespaceQueryServiceImpl dataSourceNamespaceQueryService;

  @Autowired
  protected DataSourceNamespaceTableQueryServiceImpl dataSourceNamespaceTableQueryService;

  protected SpringDataJdbcTransactionContext ctx;

  @BeforeEach
  void setUpBase() {
    cleanDatabase();
    ctx = transactionManager.single();
  }

  @AfterEach
  void tearDownBase() {
    cleanDatabase();
  }

  protected void cleanDatabase() {
    migrator.unload();
    migrator.migrate();
  }

  protected UUID createCatalog(String name) {
    UUID catalogId = UUID.randomUUID();
    catalogRepository.create(ctx, Catalog.of(catalogId, name));
    return catalogId;
  }

  protected UUID createPostgresDataSource(UUID catalogId, String name) {
    UUID dataSourceId = UUID.randomUUID();
    PostgreSql provider =
        PostgreSql.builder()
            .host(POSTGRES.getHost())
            .port(POSTGRES.getFirstMappedPort())
            .username(POSTGRES.getUsername())
            .password(POSTGRES.getPassword())
            .database(POSTGRES.getDatabaseName())
            .build();
    dataSourceRepository.create(ctx, new DataSource(dataSourceId, catalogId, name, provider));
    return dataSourceId;
  }

  protected UUID createNamespace(UUID dataSourceId, List<String> names) {
    UUID namespaceId = UUID.randomUUID();
    namespaceRepository.create(ctx, new Namespace(namespaceId, dataSourceId, names));
    return namespaceId;
  }

  protected UUID createTableWithDefaultColumns(UUID namespaceId, String tableName) {
    UUID tableId = UUID.randomUUID();
    List<Column> columns =
        Arrays.asList(
            new Column(UUID.randomUUID(), tableId, "id", DataType.Int.INSTANCE, 1, false),
            new Column(UUID.randomUUID(), tableId, "name", DataType.Text.INSTANCE, 2, false),
            new Column(UUID.randomUUID(), tableId, "email", DataType.Text.INSTANCE, 3, true));
    tableRepository.create(
        ctx, new TableDetail(new TableInfo(tableId, namespaceId, tableName), columns));
    return tableId;
  }
}
