/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.Column;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTable;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTableDetail;
import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.Table;
import com.scalar.db.analytics.api.model.TableDetail;
import com.scalar.db.analytics.api.model.TableInfo;
import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
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
import com.scalar.db.analytics.grpc.mapper.table.TableMapper;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlockingGrpcTableClientTest {
  // Constants for test data
  private static final String CATALOG_NAME = "test-catalog";
  private static final String DATA_SOURCE_NAME = "test-datasource";
  private static final UUID CATALOG_ID = UUID.randomUUID();
  private static final UUID DATA_SOURCE_ID = UUID.randomUUID();
  private static final UUID NAMESPACE_ID = UUID.randomUUID();
  private static final UUID TABLE_ID = UUID.randomUUID();
  private static final String TABLE_NAME = "test-table";
  private static final List<String> NAMESPACE_NAMES = Arrays.asList("public");

  // Data source shared across tests
  private static final DataSourceProvider TEST_PROVIDER =
      new PostgreSql("localhost", 5432, "user", "password", "test");
  private static final DataSource TEST_DATA_SOURCE =
      new DataSource(DATA_SOURCE_ID, CATALOG_ID, DATA_SOURCE_NAME, TEST_PROVIDER);

  // Data source provider shared across tests
  // Table shared across tests
  private static final TableInfo TEST_TABLE_INFO =
      new TableInfo(TABLE_ID, NAMESPACE_ID, TABLE_NAME);
  private static final Table TEST_TABLE = new Table(TEST_TABLE_INFO);

  // Namespace shared across tests
  private static final Namespace TEST_NAMESPACE =
      new Namespace(NAMESPACE_ID, DATA_SOURCE_ID, NAMESPACE_NAMES);

  // DataSourceNamespaceTable shared across tests
  private static final DataSourceNamespaceTable TEST_DATA_SOURCE_NAMESPACE_TABLE =
      new DataSourceNamespaceTable(TEST_DATA_SOURCE, TEST_NAMESPACE, TEST_TABLE);

  // Columns for table details
  private static final List<Column> TEST_COLUMNS =
      Arrays.asList(
          new Column(UUID.randomUUID(), TABLE_ID, "id", DataType.Int.INSTANCE, 1, false),
          new Column(UUID.randomUUID(), TABLE_ID, "name", DataType.Text.INSTANCE, 2, true));

  // TableDetail shared across tests
  private static final TableDetail TEST_TABLE_DETAIL =
      new TableDetail(TEST_TABLE_INFO, TEST_COLUMNS);

  // DataSourceNamespaceTableDetail shared across tests
  private static final DataSourceNamespaceTableDetail TEST_DATA_SOURCE_NAMESPACE_TABLE_DETAIL =
      new DataSourceNamespaceTableDetail(TEST_DATA_SOURCE, TEST_NAMESPACE, TEST_TABLE_DETAIL);

  @Mock private TableServiceGrpc.TableServiceBlockingStub stub;

  private TableClient tableClient;
  private final TableMapper tableMapper = TableMapper.INSTANCE;

  @BeforeEach
  void setUp() {
    tableClient = new BlockingGrpcTableClient(stub);
  }

  @Test
  void listTables_ShouldReturnTables_WhenTablesByCatalogExist() throws Exception {
    // Arrange
    ListTablesRequest expectedRequest =
        ListTablesRequest.newBuilder().setCatalogName(CATALOG_NAME).build();

    ListTablesResponse response =
        ListTablesResponse.newBuilder()
            .addTables(tableMapper.toProto(TEST_DATA_SOURCE_NAMESPACE_TABLE))
            .build();
    when(stub.listTables(expectedRequest)).thenReturn(response);

    // Act
    List<DataSourceNamespaceTable> result = tableClient.listTablesByCatalog(CATALOG_NAME);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getDataSource().getName()).isEqualTo(DATA_SOURCE_NAME);
    assertThat(result.get(0).getTable().getInfo().getName()).isEqualTo(TABLE_NAME);
    verify(stub).listTables(expectedRequest);
  }

  @Test
  void listTables_ShouldReturnEmptyList_WhenNoTablesByCatalogExist() throws Exception {
    // Arrange
    ListTablesRequest expectedRequest =
        ListTablesRequest.newBuilder().setCatalogName(CATALOG_NAME).build();

    ListTablesResponse response = ListTablesResponse.newBuilder().build();
    when(stub.listTables(expectedRequest)).thenReturn(response);

    // Act
    List<DataSourceNamespaceTable> result = tableClient.listTablesByCatalog(CATALOG_NAME);

    // Assert
    assertThat(result).isEmpty();
    verify(stub).listTables(expectedRequest);
  }

  @Test
  void listTables_ByCatalog_ShouldThrowAnalyticsException_WhenGrpcCallFails() {
    // Arrange
    ListTablesRequest expectedRequest =
        ListTablesRequest.newBuilder().setCatalogName(CATALOG_NAME).build();

    StatusRuntimeException exception = new StatusRuntimeException(Status.INTERNAL);
    when(stub.listTables(expectedRequest)).thenThrow(exception);

    // Act & Assert
    assertThatThrownBy(() -> tableClient.listTablesByCatalog(CATALOG_NAME))
        .isInstanceOf(AnalyticsException.class)
        .hasCause(exception)
        .extracting(e -> ((AnalyticsException) e).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.CLIENT_INTERNAL_ERROR);
  }

  @Test
  void listTablesByNamespace_ShouldReturnTables_WhenTablesByCatalogExist() throws Exception {
    // Arrange
    ListTablesByNamespaceRequest expectedRequest =
        ListTablesByNamespaceRequest.newBuilder()
            .setCatalogName(CATALOG_NAME)
            .setDataSourceName(DATA_SOURCE_NAME)
            .addAllNamespaceNames(NAMESPACE_NAMES)
            .build();

    ListTablesByNamespaceResponse response =
        ListTablesByNamespaceResponse.newBuilder()
            .addTables(tableMapper.toProto(TEST_DATA_SOURCE_NAMESPACE_TABLE))
            .build();
    when(stub.listTablesByNamespace(expectedRequest)).thenReturn(response);

    // Act
    List<DataSourceNamespaceTable> result =
        tableClient.listTablesByNamespace(CATALOG_NAME, DATA_SOURCE_NAME, NAMESPACE_NAMES);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getDataSource().getName()).isEqualTo(DATA_SOURCE_NAME);
    assertThat(result.get(0).getTable().getInfo().getName()).isEqualTo(TABLE_NAME);
    verify(stub).listTablesByNamespace(expectedRequest);
  }

  @Test
  void listTablesByNamespace_ShouldReturnEmptyList_WhenNoTablesByCatalogExist() throws Exception {
    // Arrange
    ListTablesByNamespaceRequest expectedRequest =
        ListTablesByNamespaceRequest.newBuilder()
            .setCatalogName(CATALOG_NAME)
            .setDataSourceName(DATA_SOURCE_NAME)
            .addAllNamespaceNames(NAMESPACE_NAMES)
            .build();

    ListTablesByNamespaceResponse response = ListTablesByNamespaceResponse.newBuilder().build();
    when(stub.listTablesByNamespace(expectedRequest)).thenReturn(response);

    // Act
    List<DataSourceNamespaceTable> result =
        tableClient.listTablesByNamespace(CATALOG_NAME, DATA_SOURCE_NAME, NAMESPACE_NAMES);

    // Assert
    assertThat(result).isEmpty();
    verify(stub).listTablesByNamespace(expectedRequest);
  }

  @Test
  void listTablesByCatalogByNamespace_ShouldThrowAnalyticsException_WhenGrpcCallFails() {
    // Arrange
    ListTablesByNamespaceRequest expectedRequest =
        ListTablesByNamespaceRequest.newBuilder()
            .setCatalogName(CATALOG_NAME)
            .setDataSourceName(DATA_SOURCE_NAME)
            .addAllNamespaceNames(NAMESPACE_NAMES)
            .build();

    StatusRuntimeException exception = new StatusRuntimeException(Status.INTERNAL);
    when(stub.listTablesByNamespace(expectedRequest)).thenThrow(exception);

    // Act & Assert
    assertThatThrownBy(
            () ->
                tableClient.listTablesByNamespace(CATALOG_NAME, DATA_SOURCE_NAME, NAMESPACE_NAMES))
        .isInstanceOf(AnalyticsException.class)
        .hasCause(exception)
        .extracting(e -> ((AnalyticsException) e).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.CLIENT_INTERNAL_ERROR);
  }

  @Test
  void describeTable_ShouldReturnTableDetail_WhenTableByNameExists() throws Exception {
    // Arrange
    DescribeTableRequest expectedRequest =
        DescribeTableRequest.newBuilder()
            .setCatalogName(CATALOG_NAME)
            .setDataSourceName(DATA_SOURCE_NAME)
            .addAllNamespaceNames(NAMESPACE_NAMES)
            .setTableName(TABLE_NAME)
            .build();

    com.scalar.db.analytics.grpc.generated.table.v1.DataSourceNamespaceTableDetail tableProto =
        tableMapper.toProto(TEST_DATA_SOURCE_NAMESPACE_TABLE_DETAIL);

    DescribeTableResponse response =
        DescribeTableResponse.newBuilder().setTable(tableProto).build();
    when(stub.describeTable(expectedRequest)).thenReturn(response);

    // Act
    Optional<DataSourceNamespaceTableDetail> result =
        tableClient.describeTableByName(
            CATALOG_NAME, DATA_SOURCE_NAME, NAMESPACE_NAMES, TABLE_NAME);

    // Assert
    assertThat(result).isPresent();
    DataSourceNamespaceTableDetail detail = result.get();
    assertThat(detail.getDataSource().getName()).isEqualTo(DATA_SOURCE_NAME);
    assertThat(detail.getNamespace().getNames()).isEqualTo(NAMESPACE_NAMES);
    assertThat(detail.getTable().getInfo().getName()).isEqualTo(TABLE_NAME);
    assertThat(detail.getTable().getColumns()).hasSize(2);
    verify(stub).describeTable(expectedRequest);
  }

  @Test
  void describeTable_ShouldReturnEmpty_WhenTableByNameDoesNotExist() throws Exception {
    // Arrange
    DescribeTableRequest expectedRequest =
        DescribeTableRequest.newBuilder()
            .setCatalogName(CATALOG_NAME)
            .setDataSourceName(DATA_SOURCE_NAME)
            .addAllNamespaceNames(NAMESPACE_NAMES)
            .setTableName(TABLE_NAME)
            .build();

    DescribeTableResponse response = DescribeTableResponse.newBuilder().build();
    when(stub.describeTable(expectedRequest)).thenReturn(response);

    // Act
    Optional<DataSourceNamespaceTableDetail> result =
        tableClient.describeTableByName(
            CATALOG_NAME, DATA_SOURCE_NAME, NAMESPACE_NAMES, TABLE_NAME);

    // Assert
    assertThat(result).isEmpty();
    verify(stub).describeTable(expectedRequest);
  }

  @Test
  void describeTable_ByName_ShouldThrowAnalyticsException_WhenGrpcCallFails() {
    // Arrange
    DescribeTableRequest expectedRequest =
        DescribeTableRequest.newBuilder()
            .setCatalogName(CATALOG_NAME)
            .setDataSourceName(DATA_SOURCE_NAME)
            .addAllNamespaceNames(NAMESPACE_NAMES)
            .setTableName(TABLE_NAME)
            .build();

    StatusRuntimeException exception = new StatusRuntimeException(Status.INTERNAL);
    when(stub.describeTable(expectedRequest)).thenThrow(exception);

    // Act & Assert
    assertThatThrownBy(
            () ->
                tableClient.describeTableByName(
                    CATALOG_NAME, DATA_SOURCE_NAME, NAMESPACE_NAMES, TABLE_NAME))
        .isInstanceOf(AnalyticsException.class)
        .hasCause(exception)
        .extracting(e -> ((AnalyticsException) e).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.CLIENT_INTERNAL_ERROR);
  }

  @Test
  void describeTableById_ShouldReturnTableDetail_WhenTableByNameExists() throws Exception {
    // Arrange
    DescribeTableByIdRequest expectedRequest =
        DescribeTableByIdRequest.newBuilder().setTableId(TABLE_ID.toString()).build();

    com.scalar.db.analytics.grpc.generated.table.v1.DataSourceNamespaceTableDetail tableProto =
        tableMapper.toProto(TEST_DATA_SOURCE_NAMESPACE_TABLE_DETAIL);

    DescribeTableByIdResponse response =
        DescribeTableByIdResponse.newBuilder().setTable(tableProto).build();
    when(stub.describeTableById(expectedRequest)).thenReturn(response);

    // Act
    Optional<DataSourceNamespaceTableDetail> result = tableClient.describeTableById(TABLE_ID);

    // Assert
    assertThat(result).isPresent();
    DataSourceNamespaceTableDetail detail = result.get();
    assertThat(detail.getDataSource().getName()).isEqualTo(DATA_SOURCE_NAME);
    assertThat(detail.getNamespace().getNames()).isEqualTo(NAMESPACE_NAMES);
    assertThat(detail.getTable().getInfo().getName()).isEqualTo(TABLE_NAME);
    assertThat(detail.getTable().getColumns()).hasSize(2);
    verify(stub).describeTableById(expectedRequest);
  }

  @Test
  void describeTableById_ShouldReturnEmpty_WhenTableByNameDoesNotExist() throws Exception {
    // Arrange
    DescribeTableByIdRequest expectedRequest =
        DescribeTableByIdRequest.newBuilder().setTableId(TABLE_ID.toString()).build();

    DescribeTableByIdResponse response = DescribeTableByIdResponse.newBuilder().build();
    when(stub.describeTableById(expectedRequest)).thenReturn(response);

    // Act
    Optional<DataSourceNamespaceTableDetail> result = tableClient.describeTableById(TABLE_ID);

    // Assert
    assertThat(result).isEmpty();
    verify(stub).describeTableById(expectedRequest);
  }

  @Test
  void describeTableByNameById_ShouldThrowAnalyticsException_WhenGrpcCallFails() {
    // Arrange
    DescribeTableByIdRequest expectedRequest =
        DescribeTableByIdRequest.newBuilder().setTableId(TABLE_ID.toString()).build();

    StatusRuntimeException exception = new StatusRuntimeException(Status.INTERNAL);
    when(stub.describeTableById(expectedRequest)).thenThrow(exception);

    // Act & Assert
    assertThatThrownBy(() -> tableClient.describeTableById(TABLE_ID))
        .isInstanceOf(AnalyticsException.class)
        .hasCause(exception)
        .extracting(e -> ((AnalyticsException) e).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.CLIENT_INTERNAL_ERROR);
  }
}
