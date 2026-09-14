/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.datasource.provider.DynamoDbProvider;
import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Databricks;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.MySql;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Oracle;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Snowflake;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.SqlServer;
import com.scalar.db.analytics.repository.impl.spring.support.AbstractScalarDbIntegrationTest;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DataSourceRepositoryIntegrationTest extends AbstractScalarDbIntegrationTest {

  private UUID catalogId;

  @BeforeEach
  void setUpCatalog() {
    catalogId = createCatalog("catalog-ds");
  }

  @Test
  void createShouldPersistDataSource() {
    DataSource detail = postgresDetail("primary-ds");

    dataSourceRepository.create(ctx, detail);

    Optional<DataSource> found = dataSourceRepository.findById(ctx, detail.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("primary-ds");
  }

  @Test
  void createDuplicateShouldThrowEntityAlreadyExists() {
    dataSourceRepository.create(ctx, postgresDetail("dup-ds"));

    assertThatThrownBy(() -> dataSourceRepository.create(ctx, postgresDetail("dup-ds")))
        .isInstanceOf(AnalyticsException.class)
        .extracting(ex -> ((AnalyticsException) ex).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.DATA_SOURCE_ALREADY_EXISTS);
  }

  @Test
  void findByIdShouldReturnEmptyWhenMissing() {
    assertThat(dataSourceRepository.findById(ctx, UUID.randomUUID())).isEmpty();
  }

  @Test
  void findDetailByIdShouldReturnEmptyWhenMissing() {
    assertThat(dataSourceRepository.findById(ctx, UUID.randomUUID())).isEmpty();
  }

  @Test
  void deleteByIdShouldRemoveDataSource() {
    DataSource detail = postgresDetail("delete-ds");
    dataSourceRepository.create(ctx, detail);

    dataSourceRepository.deleteById(ctx, detail.getId());

    assertThat(dataSourceRepository.findById(ctx, detail.getId())).isEmpty();
  }

  @Test
  void deleteByIdShouldNotThrowWhenMissing() {
    assertThatCode(() -> dataSourceRepository.deleteById(ctx, UUID.randomUUID()))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldRoundTripPostgreSqlProvider() {
    assertPostgresProviderPersistence("pg-ds");
  }

  @Test
  void shouldRoundTripMySqlProvider() {
    DataSource detail =
        new DataSource(
            UUID.randomUUID(),
            catalogId,
            "mysql-ds",
            MySql.builder()
                .host("mysql-host")
                .port(3306)
                .username("mysql-user")
                .password("mysql-pass")
                .database("mysql-db")
                .build());

    dataSourceRepository.create(ctx, detail);

    Optional<DataSource> stored = dataSourceRepository.findById(ctx, detail.getId());
    assertThat(stored).isPresent();
    MySql provider = (MySql) stored.get().getProvider();
    assertThat(provider.getHost()).isEqualTo("mysql-host");
    assertThat(provider.getDatabase()).isEqualTo("mysql-db");
  }

  @Test
  void shouldRoundTripSqlServerProvider() {
    DataSource detail =
        new DataSource(
            UUID.randomUUID(),
            catalogId,
            "sqlserver-ds",
            SqlServer.builder()
                .host("sqlserver-host")
                .port(1433)
                .username("sa")
                .password("pass")
                .database("master")
                .build());

    dataSourceRepository.create(ctx, detail);

    Optional<DataSource> stored = dataSourceRepository.findById(ctx, detail.getId());
    assertThat(stored).isPresent();
    SqlServer provider = (SqlServer) stored.get().getProvider();
    assertThat(provider.getHost()).isEqualTo("sqlserver-host");
  }

  @Test
  void shouldRoundTripOracleProvider() {
    DataSource detail =
        new DataSource(
            UUID.randomUUID(),
            catalogId,
            "oracle-ds",
            Oracle.builder()
                .host("oracle-host")
                .port(1521)
                .username("system")
                .password("oracle-pass")
                .serviceName("ORCL")
                .build());

    dataSourceRepository.create(ctx, detail);

    Optional<DataSource> stored = dataSourceRepository.findById(ctx, detail.getId());
    assertThat(stored).isPresent();
    Oracle provider = (Oracle) stored.get().getProvider();
    assertThat(provider.getServiceName()).isEqualTo("ORCL");
  }

  @Test
  void shouldRoundTripDatabricksProvider() {
    DataSource detail =
        new DataSource(
            UUID.randomUUID(),
            catalogId,
            "databricks-ds",
            new Databricks("host", 443, "/sql/path", "client", "secret", "catalog"));

    dataSourceRepository.create(ctx, detail);

    Optional<DataSource> stored = dataSourceRepository.findById(ctx, detail.getId());
    assertThat(stored).isPresent();
    Databricks provider = (Databricks) stored.get().getProvider();
    assertThat(provider.getHttpPath()).isEqualTo("/sql/path");
  }

  @Test
  void shouldRoundTripSnowflakeProvider() {
    DataSource detail =
        new DataSource(
            UUID.randomUUID(),
            catalogId,
            "snowflake-ds",
            Snowflake.builder()
                .account("account")
                .username("snow-user")
                .password("snow-pass")
                .database("snow-db")
                .build());

    dataSourceRepository.create(ctx, detail);

    Optional<DataSource> stored = dataSourceRepository.findById(ctx, detail.getId());
    assertThat(stored).isPresent();
    Snowflake provider = (Snowflake) stored.get().getProvider();
    assertThat(provider.getAccount()).isEqualTo("account");
  }

  @Test
  void shouldRoundTripScalarDbProvider() {
    DataSource detail =
        new DataSource(
            UUID.randomUUID(),
            catalogId,
            "scalardb-ds",
            ScalarDbProvider.builder().configs(Map.of("scalar.db.storage", "cassandra")).build());

    dataSourceRepository.create(ctx, detail);

    Optional<DataSource> stored = dataSourceRepository.findById(ctx, detail.getId());
    assertThat(stored).isPresent();
    ScalarDbProvider provider = (ScalarDbProvider) stored.get().getProvider();
    assertThat(provider.getConfigs()).containsEntry("scalar.db.storage", "cassandra");
  }

  @Test
  void shouldRoundTripDynamoDbProvider() {
    DataSource detail =
        new DataSource(
            UUID.randomUUID(),
            catalogId,
            "dynamodb-ds",
            DynamoDbProvider.builder()
                .region("ap-northeast-1")
                .endpoint("http://localhost:8000")
                .build());

    dataSourceRepository.create(ctx, detail);

    Optional<DataSource> stored = dataSourceRepository.findById(ctx, detail.getId());
    assertThat(stored).isPresent();
    DynamoDbProvider provider = (DynamoDbProvider) stored.get().getProvider();
    assertThat(provider.getRegion()).isEqualTo("ap-northeast-1");
    assertThat(provider.getEndpoint()).isEqualTo("http://localhost:8000");
  }

  @Test
  void findDetailByIdShouldReturnDataSourceWithProvider() {
    DataSource detail = postgresDetail("detail-ds");
    dataSourceRepository.create(ctx, detail);

    Optional<DataSource> stored = dataSourceRepository.findById(ctx, detail.getId());
    assertThat(stored).isPresent();
    assertThat(stored.get().getProvider()).isInstanceOf(PostgreSql.class);
  }

  private DataSource postgresDetail(String name) {
    return new DataSource(
        UUID.randomUUID(),
        catalogId,
        name,
        PostgreSql.builder()
            .host("localhost")
            .port(5432)
            .username("postgres")
            .password("pass")
            .database("analytics")
            .build());
  }

  @Test
  void findByCatalogIdAndNameShouldReturnDataSource() {
    DataSource detail = postgresDetail("lookup-ds");
    dataSourceRepository.create(ctx, detail);

    Optional<DataSource> found =
        dataSourceRepository.findByCatalogIdAndName(ctx, catalogId, "lookup-ds");

    assertThat(found).isPresent();
    assertThat(found.get().getId()).isEqualTo(detail.getId());
    assertThat(found.get().getName()).isEqualTo("lookup-ds");
  }

  @Test
  void findByCatalogIdAndNameShouldReturnEmptyWhenNotFound() {
    assertThat(dataSourceRepository.findByCatalogIdAndName(ctx, catalogId, "nonexistent"))
        .isEmpty();
  }

  @Test
  void findByCatalogIdAndNameShouldReturnEmptyWhenCatalogMismatch() {
    dataSourceRepository.create(ctx, postgresDetail("mismatch-ds"));

    assertThat(dataSourceRepository.findByCatalogIdAndName(ctx, UUID.randomUUID(), "mismatch-ds"))
        .isEmpty();
  }

  private void assertPostgresProviderPersistence(String name) {
    DataSource detail = postgresDetail(name);
    dataSourceRepository.create(ctx, detail);

    Optional<DataSource> stored = dataSourceRepository.findById(ctx, detail.getId());
    assertThat(stored).isPresent();
    PostgreSql provider = (PostgreSql) stored.get().getProvider();
    assertThat(provider.getDatabase()).isEqualTo("analytics");
  }
}
