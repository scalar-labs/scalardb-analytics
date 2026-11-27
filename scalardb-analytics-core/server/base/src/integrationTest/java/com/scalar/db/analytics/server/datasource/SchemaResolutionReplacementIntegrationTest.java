/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.api.model.DataSourceNamespaceTable;
import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest;
import com.scalar.db.analytics.api.request.schema.ColumnSchema;
import com.scalar.db.analytics.api.request.schema.DataSourceSchema;
import com.scalar.db.analytics.api.request.schema.NamespaceSchema;
import com.scalar.db.analytics.api.request.schema.TableSchema;
import com.scalar.db.analytics.server.support.UseCaseIntegrationTestBase;
import com.scalar.db.analytics.service.SchemaResolution;
import com.scalar.db.analytics.service.datasource.NoOpSchemaResolver;
import com.scalar.db.analytics.usecase.CatalogUseCase;
import com.scalar.db.analytics.usecase.DataSourceUseCase;
import com.scalar.db.analytics.usecase.TableUseCase;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Verifies that an environment can replace how the server resolves a data source's schema by
 * declaring a {@link SchemaResolution} bean.
 */
@Import(SchemaResolutionReplacementIntegrationTest.EnvironmentSchemaResolution.class)
class SchemaResolutionReplacementIntegrationTest extends UseCaseIntegrationTestBase {

  private static final String CATALOG_NAME = "schema_resolution_replacement";

  /** The schema the environment knows for the source, without connecting to it. */
  private static final DataSourceSchema ENVIRONMENT_SCHEMA =
      DataSourceSchema.builder()
          .namespaces(
              List.of(
                  NamespaceSchema.builder()
                      .names(List.of("public"))
                      .tables(
                          List.of(
                              TableSchema.builder()
                                  .name("orders")
                                  .columns(
                                      List.of(
                                          ColumnSchema.builder()
                                              .name("id")
                                              .type(DataType.Int.INSTANCE)
                                              .nullable(false)
                                              .build()))
                                  .build()))
                      .build()))
          .build();

  @TestConfiguration
  static class EnvironmentSchemaResolution {
    @Bean
    SchemaResolution schemaResolution() {
      return (dataSourceId, provider, schema) ->
          new NoOpSchemaResolver(dataSourceId, ENVIRONMENT_SCHEMA).resolveSchema();
    }
  }

  @Autowired private CatalogUseCase catalogUseCase;
  @Autowired private DataSourceUseCase dataSourceUseCase;
  @Autowired private TableUseCase tableUseCase;

  @Test
  void register_withDeclaredSchemaResolution_shouldNotConnectToSource() {
    UUID adminId = adminUserId();
    catalogUseCase.createCatalog(adminId, CATALOG_NAME);
    // The default resolution would connect to this host and fail; the declared one does not.
    PostgreSql unreachable =
        PostgreSql.builder()
            .host("unreachable.invalid")
            .port(5432)
            .username("user")
            .password("password")
            .database("db")
            .build();

    dataSourceUseCase.register(
        adminId, new RegisterDataSourceRequest(CATALOG_NAME, "source", unreachable, null));

    assertThat(tableUseCase.listTables(adminId, CATALOG_NAME))
        .extracting(DataSourceNamespaceTable::getTable)
        .extracting(table -> table.getInfo().getName())
        .containsExactly("orders");
  }
}
