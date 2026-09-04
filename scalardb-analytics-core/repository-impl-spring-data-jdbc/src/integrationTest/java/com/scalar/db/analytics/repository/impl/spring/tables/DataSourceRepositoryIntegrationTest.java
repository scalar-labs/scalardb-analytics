/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.tables;

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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DataSourceRepositoryIntegrationTest extends AbstractScalarDbIntegrationTest {

  private UUID catalogId;

  @BeforeEach
  void setupCatalog() {
    catalogId = createCatalog("catalog-ds");
  }

  @Test
  void createAndFindById() {
    DataSource detail = postgresDetail("primary-ds");

    dataSourceRepository.create(ctx, detail);

    Optional<DataSource> found = dataSourceRepository.findById(ctx, detail.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("primary-ds");
  }

  @Test
  void createDuplicateShouldThrowException() {
    dataSourceRepository.create(ctx, postgresDetail("dup-ds"));

    DataSource duplicate = postgresDetail("dup-ds");

    assertThatThrownBy(() -> dataSourceRepository.create(ctx, duplicate))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            ex ->
                assertThat(((AnalyticsException) ex).getErrorCode())
                    .isEqualTo(AnalyticsErrorCode.DATA_SOURCE_ALREADY_EXISTS));
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
  void queryServiceShouldListByCatalogName() {
    dataSourceRepository.create(ctx, postgresDetail("pg-ds"));
    dataSourceRepository.create(ctx, mysqlDetail("mysql-ds"));

    List<DataSource> dataSources = dataSourceQueryService.listByCatalogName(ctx, "catalog-ds");

    assertThat(dataSources).hasSize(2);
    assertThat(dataSources)
        .extracting(ds -> ds.getName())
        .containsExactlyInAnyOrder("pg-ds", "mysql-ds");
  }

  @Test
  void shouldDeserializePostgreSqlProvider() {
    DataSource detail = postgresDetail("pg-ds");

    dataSourceRepository.create(ctx, detail);

    assertPostgreSqlProvider(detail);
  }

  @Test
  void shouldDeserializeMySqlProvider() {
    DataSource detail = mysqlDetail("mysql-ds");

    dataSourceRepository.create(ctx, detail);

    Optional<DataSource> found = dataSourceRepository.findById(ctx, detail.getId());
    assertThat(found).isPresent();
    MySql provider = (MySql) found.get().getProvider();
    assertThat(provider.getHost()).isEqualTo("mysql-host");
    assertThat(provider.getPort()).isEqualTo(3306);
    assertThat(provider.getDatabase()).isEqualTo("mysql-db");
  }

  @Test
  void shouldDeserializeSqlServerProvider() {
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

    Optional<DataSource> found = dataSourceRepository.findById(ctx, detail.getId());
    assertThat(found).isPresent();
    SqlServer provider = (SqlServer) found.get().getProvider();
    assertThat(provider.getHost()).isEqualTo("sqlserver-host");
    assertThat(provider.getDatabase()).isEqualTo("master");
  }

  @Test
  void shouldDeserializeOracleProvider() {
    DataSource detail =
        new DataSource(
            UUID.randomUUID(),
            catalogId,
            "oracle-ds",
            Oracle.builder()
                .host("oracle-host")
                .port(1521)
                .username("oracle-user")
                .password("oracle-pass")
                .serviceName("ORCL")
                .build());

    dataSourceRepository.create(ctx, detail);

    Optional<DataSource> found = dataSourceRepository.findById(ctx, detail.getId());
    assertThat(found).isPresent();
    Oracle provider = (Oracle) found.get().getProvider();
    assertThat(provider.getServiceName()).isEqualTo("ORCL");
  }

  @Test
  void shouldDeserializeDatabricksProvider() {
    DataSource detail =
        new DataSource(
            UUID.randomUUID(),
            catalogId,
            "databricks-ds",
            new Databricks("host", 443, "/sql/path", "client", "secret", "catalog"));

    dataSourceRepository.create(ctx, detail);

    Optional<DataSource> found = dataSourceRepository.findById(ctx, detail.getId());
    assertThat(found).isPresent();
    Databricks provider = (Databricks) found.get().getProvider();
    assertThat(provider.getHttpPath()).isEqualTo("/sql/path");
  }

  @Test
  void shouldDeserializeSnowflakeProvider() {
    DataSource detail =
        new DataSource(
            UUID.randomUUID(),
            catalogId,
            "snowflake-ds",
            Snowflake.builder()
                .account("account")
                .username("sf-user")
                .password("sf-pass")
                .database("sf-db")
                .build());

    dataSourceRepository.create(ctx, detail);

    Optional<DataSource> found = dataSourceRepository.findById(ctx, detail.getId());
    assertThat(found).isPresent();
    Snowflake provider = (Snowflake) found.get().getProvider();
    assertThat(provider.getAccount()).isEqualTo("account");
  }

  @Test
  void shouldDeserializeScalarDbProvider() {
    DataSource detail =
        new DataSource(
            UUID.randomUUID(),
            catalogId,
            "scalardb-ds",
            ScalarDbProvider.builder().configs(Map.of("scalar.db.storage", "cassandra")).build());

    dataSourceRepository.create(ctx, detail);

    Optional<DataSource> found = dataSourceRepository.findById(ctx, detail.getId());
    assertThat(found).isPresent();
    ScalarDbProvider provider = (ScalarDbProvider) found.get().getProvider();
    assertThat(provider.getConfigs()).containsEntry("scalar.db.storage", "cassandra");
  }

  @Test
  void shouldDeserializeDynamoDbProvider() {
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

    Optional<DataSource> found = dataSourceRepository.findById(ctx, detail.getId());
    assertThat(found).isPresent();
    DynamoDbProvider provider = (DynamoDbProvider) found.get().getProvider();
    assertThat(provider.getRegion()).isEqualTo("ap-northeast-1");
    assertThat(provider.getEndpoint()).isEqualTo("http://localhost:8000");
  }

  private DataSource postgresDetail(String name) {
    PostgreSql provider =
        PostgreSql.builder()
            .host("postgres-host")
            .port(5432)
            .username("postgres-user")
            .password("postgres-pass")
            .database("postgres-db")
            .build();
    return new DataSource(UUID.randomUUID(), catalogId, name, provider);
  }

  private DataSource mysqlDetail(String name) {
    MySql provider =
        MySql.builder()
            .host("mysql-host")
            .port(3306)
            .username("mysql-user")
            .password("mysql-pass")
            .database("mysql-db")
            .build();
    return new DataSource(UUID.randomUUID(), catalogId, name, provider);
  }

  private void assertPostgreSqlProvider(DataSource dataSource) {
    Optional<DataSource> found = dataSourceRepository.findById(ctx, dataSource.getId());
    assertThat(found).isPresent();
    PostgreSql provider = (PostgreSql) found.get().getProvider();
    assertThat(provider.getHost()).isEqualTo("postgres-host");
    assertThat(provider.getDatabase()).isEqualTo("postgres-db");
  }
}
