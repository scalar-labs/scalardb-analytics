/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.TableDetail;
import com.scalar.db.analytics.api.model.datasource.provider.DynamoDbProvider;
import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;
import com.scalar.db.analytics.api.request.schema.ColumnSchema;
import com.scalar.db.analytics.api.request.schema.DataSourceSchema;
import com.scalar.db.analytics.api.request.schema.NamespaceSchema;
import com.scalar.db.analytics.api.request.schema.TableSchema;
import com.scalar.db.analytics.service.SuppliedSchemaRejectedException;
import com.scalar.db.analytics.service.SuppliedSchemaRejectedException.Reason;
import com.scalar.db.analytics.service.datasource.InvalidDataSourceConfigurationException;
import com.scalar.db.analytics.service.datasource.ResolvedSchema;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DefaultSchemaResolutionTest {

  private final DefaultSchemaResolution resolution = new DefaultSchemaResolution();
  private final UUID dataSourceId = UUID.randomUUID();

  @Test
  void resolve_manualProviderWithSchema_shouldUseSuppliedSchema() throws Exception {
    ResolvedSchema resolved =
        resolution.resolve(
            dataSourceId,
            DynamoDbProvider.builder().region("us-east-1").build(),
            createSchema("ns", "tbl", "id"));

    assertThat(resolved.getNamespaces())
        .singleElement()
        .satisfies(
            namespace -> {
              assertThat(namespace.getDataSourceId()).isEqualTo(dataSourceId);
              assertThat(namespace.getNames()).containsExactly("ns");
            });
    Namespace namespace = resolved.getNamespaces().getFirst();
    assertThat(resolved.getTables())
        .singleElement()
        .satisfies(
            table -> {
              assertThat(table.getInfo().getNamespaceId()).isEqualTo(namespace.getId());
              assertThat(table.getInfo().getName()).isEqualTo("tbl");
            });
    TableDetail table = resolved.getTables().getFirst();
    assertThat(table.getColumns()).hasSize(1);
  }

  @Test
  void resolve_manualProviderWithoutSchema_shouldRejectAsRequired() {
    assertThatThrownBy(
            () ->
                resolution.resolve(
                    dataSourceId, DynamoDbProvider.builder().region("us-east-1").build(), null))
        .isInstanceOfSatisfying(
            SuppliedSchemaRejectedException.class,
            e -> assertThat(e.getReason()).isEqualTo(Reason.REQUIRED_FOR_PROVIDER));
  }

  @Test
  void resolve_autoProviderWithSchema_shouldRejectAsNotAllowed() {
    PostgreSql provider =
        PostgreSql.builder()
            .host("localhost")
            .port(5432)
            .username("user")
            .password("pass")
            .database("db")
            .build();

    assertThatThrownBy(
            () -> resolution.resolve(dataSourceId, provider, createSchema("ns", "tbl", "id")))
        .isInstanceOfSatisfying(
            SuppliedSchemaRejectedException.class,
            e -> assertThat(e.getReason()).isEqualTo(Reason.NOT_ALLOWED_FOR_PROVIDER));
  }

  @Test
  void resolve_scalarDbProviderWithConfigRejectedByScalarDb_shouldThrowInvalidConfiguration() {
    // ScalarDB throws IllegalArgumentException for a storage it does not know. The real
    // ScalarDbSchemaResolver runs here, and fails before connecting to anything.
    ScalarDbProvider provider =
        ScalarDbProvider.builder().configs(Map.of("scalar.db.storage", "no-such-storage")).build();

    assertThatThrownBy(() -> resolution.resolve(dataSourceId, provider, null))
        .isInstanceOf(InvalidDataSourceConfigurationException.class)
        .hasCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void resolve_scalarDbProviderWithNonNumericProperty_shouldThrowInvalidConfiguration() {
    // ScalarDB throws IllegalArgumentException for a property that must be a number. The JDBC
    // storage validates its configuration before opening any connection.
    ScalarDbProvider provider =
        ScalarDbProvider.builder()
            .configs(
                Map.of(
                    "scalar.db.storage",
                    "jdbc",
                    "scalar.db.contact_points",
                    "jdbc:postgresql://127.0.0.1:1/db",
                    "scalar.db.jdbc.connection_pool.min_idle",
                    "not-a-number"))
            .build();

    assertThatThrownBy(() -> resolution.resolve(dataSourceId, provider, null))
        .isInstanceOf(InvalidDataSourceConfigurationException.class)
        .hasCauseInstanceOf(IllegalArgumentException.class);
  }

  private static DataSourceSchema createSchema(String namespace, String table, String column) {
    return DataSourceSchema.builder()
        .namespaces(
            List.of(
                NamespaceSchema.builder()
                    .names(List.of(namespace))
                    .tables(
                        List.of(
                            TableSchema.builder()
                                .name(table)
                                .columns(
                                    List.of(
                                        ColumnSchema.builder()
                                            .name(column)
                                            .type(DataType.Text.INSTANCE)
                                            .nullable(false)
                                            .build()))
                                .build()))
                    .build()))
        .build();
  }
}
