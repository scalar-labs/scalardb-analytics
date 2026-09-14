/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.model.Column;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTable;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTableDetail;
import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.Table;
import com.scalar.db.analytics.api.model.TableDetail;
import com.scalar.db.analytics.api.model.TableInfo;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;
import com.scalar.db.analytics.grpc.generated.table.v1.DescribeTableByIdRequest;
import com.scalar.db.analytics.grpc.generated.table.v1.DescribeTableByIdResponse;
import com.scalar.db.analytics.grpc.generated.table.v1.DescribeTableRequest;
import com.scalar.db.analytics.grpc.generated.table.v1.DescribeTableResponse;
import com.scalar.db.analytics.grpc.generated.table.v1.ListTablesByNamespaceRequest;
import com.scalar.db.analytics.grpc.generated.table.v1.ListTablesByNamespaceResponse;
import com.scalar.db.analytics.grpc.generated.table.v1.ListTablesRequest;
import com.scalar.db.analytics.grpc.generated.table.v1.ListTablesResponse;
import com.scalar.db.analytics.grpc.generated.table.v1.TableServiceGrpc;
import com.scalar.db.analytics.grpc.generated.table.v1.TableServiceGrpc.TableServiceBlockingStub;
import com.scalar.db.analytics.grpc.mapper.table.TableMapper;
import com.scalar.db.analytics.usecase.TableUseCase;
import io.grpc.Channel;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TableServiceImplTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private TableUseCase useCase;
  private TableServiceBlockingStub stub;
  private final TableMapper tableMapper = TableMapper.INSTANCE;

  private final DataSource testDataSource =
      DataSource.create(
          UUID.randomUUID(),
          "dataSource1",
          new PostgreSql("test_host", 54321, "test_user", "test_password", "test_database"));
  private final Namespace testNamespace =
      Namespace.create(testDataSource.getId(), Collections.singletonList("test_namespace"));
  private final TableInfo testTableInfo1 = TableInfo.create(testNamespace.getId(), "test_table_1");
  private final TableInfo testTableInfo2 = TableInfo.create(testNamespace.getId(), "test_table_2");
  private final Table testTable1 = new Table(testTableInfo1);
  private final Table testTable2 = new Table(testTableInfo2);
  private final TableDetail testTableDetail1 =
      new TableDetail(
          testTableInfo1,
          Arrays.asList(
              new Column(
                  UUID.randomUUID(),
                  testTableInfo1.getId(),
                  "column1",
                  DataType.Int.INSTANCE,
                  0,
                  false),
              new Column(
                  UUID.randomUUID(),
                  testTableInfo1.getId(),
                  "column2",
                  DataType.Text.INSTANCE,
                  1,
                  true)));

  @BeforeEach
  void setUp() throws IOException {
    useCase = mock(TableUseCase.class);
    TableServiceImpl service = new TableServiceImpl(useCase);

    String serverName = InProcessServerBuilder.generateName();
    InProcessServerBuilder.forName(serverName)
        .addService(service)
        .intercept(userIdInterceptor())
        .build()
        .start();

    Channel channel = InProcessChannelBuilder.forName(serverName).build();
    stub = TableServiceGrpc.newBlockingStub(channel);
  }

  @Test
  void listTables_shouldReturnListOfTables() {
    ListTablesRequest request =
        ListTablesRequest.newBuilder().setCatalogName("test_catalog").build();

    DataSourceNamespaceTable table1 =
        new DataSourceNamespaceTable(testDataSource, testNamespace, testTable1);
    DataSourceNamespaceTable table2 =
        new DataSourceNamespaceTable(testDataSource, testNamespace, testTable2);
    when(useCase.listTables(USER_ID, "test_catalog")).thenReturn(Arrays.asList(table1, table2));

    ListTablesResponse response = stub.listTables(request);

    assertThat(response.getTablesList())
        .hasSize(2)
        .map(tableMapper::toDomain)
        .containsExactlyInAnyOrder(table1, table2);
  }

  @Test
  void listTablesByNamespace_shouldReturnListOfTables() {
    ListTablesByNamespaceRequest request =
        ListTablesByNamespaceRequest.newBuilder()
            .setCatalogName("test_catalog")
            .setDataSourceName("test_data_source")
            .addNamespaceNames("test_namespace")
            .build();

    DataSourceNamespaceTable table1 =
        new DataSourceNamespaceTable(testDataSource, testNamespace, testTable1);
    DataSourceNamespaceTable table2 =
        new DataSourceNamespaceTable(testDataSource, testNamespace, testTable2);
    when(useCase.listTablesByNamespace(
            USER_ID,
            "test_catalog",
            "test_data_source",
            Collections.singletonList("test_namespace")))
        .thenReturn(Arrays.asList(table1, table2));

    ListTablesByNamespaceResponse response = stub.listTablesByNamespace(request);

    assertThat(response.getTablesList())
        .hasSize(2)
        .map(tableMapper::toDomain)
        .containsExactlyInAnyOrder(table1, table2);
  }

  @Test
  void describeTable_shouldReturnTableDetails() {
    DescribeTableRequest request =
        DescribeTableRequest.newBuilder()
            .setCatalogName("test_catalog")
            .setDataSourceName("test_data_source")
            .addNamespaceNames("test_namespace")
            .setTableName("test_table")
            .build();

    DataSourceNamespaceTableDetail tableDetail =
        new DataSourceNamespaceTableDetail(testDataSource, testNamespace, testTableDetail1);
    when(useCase.describeTable(
            USER_ID,
            "test_catalog",
            "test_data_source",
            Collections.singletonList("test_namespace"),
            "test_table"))
        .thenReturn(Optional.of(tableDetail));

    DescribeTableResponse response = stub.describeTable(request);

    DataSourceNamespaceTableDetail got = tableMapper.toDomain(response.getTable());
    assertThat(got).isEqualTo(tableDetail);
  }

  @Test
  void describeTableById_shouldReturnTableDetails() {
    DescribeTableByIdRequest request =
        DescribeTableByIdRequest.newBuilder()
            .setTableId(testTableDetail1.getInfo().getId().toString())
            .build();

    DataSourceNamespaceTableDetail tableDetail =
        new DataSourceNamespaceTableDetail(testDataSource, testNamespace, testTableDetail1);
    when(useCase.describeTableById(USER_ID, testTableDetail1.getInfo().getId()))
        .thenReturn(Optional.of(tableDetail));

    DescribeTableByIdResponse response = stub.describeTableById(request);

    DataSourceNamespaceTableDetail got = tableMapper.toDomain(response.getTable());
    assertThat(got).isEqualTo(tableDetail);
  }

  private static ServerInterceptor userIdInterceptor() {
    return new ServerInterceptor() {
      @Override
      public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
          ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        Context ctx =
            Context.current().withValue(AuthenticationInterceptor.AUTHENTICATED_USER_ID, USER_ID);
        return Contexts.interceptCall(ctx, call, headers, next);
      }
    };
  }
}
