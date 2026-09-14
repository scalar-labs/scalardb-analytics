/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest;
import com.scalar.db.analytics.grpc.generated.datasource.v1.DataSourceServiceGrpc;
import com.scalar.db.analytics.grpc.generated.datasource.v1.FindDataSourceRequest;
import com.scalar.db.analytics.grpc.generated.datasource.v1.FindDataSourceResponse;
import com.scalar.db.analytics.grpc.generated.datasource.v1.ListDataSourcesRequest;
import com.scalar.db.analytics.grpc.generated.datasource.v1.ListDataSourcesResponse;
import com.scalar.db.analytics.grpc.generated.datasource.v1.RegisterResponse;
import com.scalar.db.analytics.grpc.mapper.datasource.DataSourceMapper;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlockingGrpcDataSourceClientTest {
  private static final String CATALOG_NAME = "test-catalog";
  private static final String DATA_SOURCE_NAME = "test-datasource";
  private static final String DATA_SOURCE2_NAME = "test-datasource2";
  private static final UUID CATALOG_ID = UUID.randomUUID();
  private static final UUID DATA_SOURCE_ID = UUID.randomUUID();
  private static final UUID DATA_SOURCE2_ID = UUID.randomUUID();

  // Data source provider shared across tests
  private static final DataSourceProvider TEST_PROVIDER =
      new PostgreSql("localhost", 5432, "user", "password", "test");

  // Data source and data source detail shared across tests
  private static final DataSource TEST_DATA_SOURCE =
      new DataSource(DATA_SOURCE_ID, CATALOG_ID, DATA_SOURCE_NAME, TEST_PROVIDER);
  private static final DataSource TEST_DATA_SOURCE2 =
      new DataSource(
          DATA_SOURCE2_ID,
          CATALOG_ID,
          DATA_SOURCE2_NAME,
          new PostgreSql("localhost", 5433, "user2", "password", "test2"));

  @Mock private DataSourceServiceGrpc.DataSourceServiceBlockingStub stub;

  private DataSourceClient dataSourceClient;
  private final DataSourceMapper dataSourceMapper = DataSourceMapper.INSTANCE;

  @BeforeEach
  void setUp() {
    dataSourceClient = new BlockingGrpcDataSourceClient(stub);
  }

  @Test
  void findDataSource_ShouldReturnDataSource_WhenDataSourceByNameExists() throws Exception {
    // Arrange
    FindDataSourceRequest expectedRequest =
        FindDataSourceRequest.newBuilder()
            .setCatalogName(CATALOG_NAME)
            .setDataSourceName(DATA_SOURCE_NAME)
            .build();

    FindDataSourceResponse response =
        FindDataSourceResponse.newBuilder()
            .setDataSource(dataSourceMapper.toProto(TEST_DATA_SOURCE))
            .build();
    when(stub.findDataSource(expectedRequest)).thenReturn(response);

    // Act
    Optional<DataSource> result =
        dataSourceClient.findDataSourceByName(CATALOG_NAME, DATA_SOURCE_NAME);

    // Assert
    assertThat(result).isPresent().hasValue(TEST_DATA_SOURCE);
    verify(stub).findDataSource(expectedRequest);
  }

  @Test
  void findDataSource_ShouldReturnEmpty_WhenDataSourceByNameDoesNotExist() throws Exception {
    // Arrange
    FindDataSourceRequest expectedRequest =
        FindDataSourceRequest.newBuilder()
            .setCatalogName(CATALOG_NAME)
            .setDataSourceName(DATA_SOURCE_NAME)
            .build();

    FindDataSourceResponse response = FindDataSourceResponse.newBuilder().build();
    when(stub.findDataSource(expectedRequest)).thenReturn(response);

    // Act
    Optional<DataSource> result =
        dataSourceClient.findDataSourceByName(CATALOG_NAME, DATA_SOURCE_NAME);

    // Assert
    assertThat(result).isEmpty();
    verify(stub).findDataSource(expectedRequest);
  }

  @Test
  void findDataSource_ByName_ShouldThrowAnalyticsException_WhenGrpcCallFails() {
    // Arrange
    FindDataSourceRequest expectedRequest =
        FindDataSourceRequest.newBuilder()
            .setCatalogName(CATALOG_NAME)
            .setDataSourceName(DATA_SOURCE_NAME)
            .build();

    StatusRuntimeException exception = new StatusRuntimeException(Status.INTERNAL);
    when(stub.findDataSource(expectedRequest)).thenThrow(exception);

    // Act & Assert
    assertThatThrownBy(() -> dataSourceClient.findDataSourceByName(CATALOG_NAME, DATA_SOURCE_NAME))
        .isInstanceOf(AnalyticsException.class)
        .hasCause(exception)
        .extracting(e -> ((AnalyticsException) e).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.CLIENT_INTERNAL_ERROR);
  }

  @Test
  void listDataSources_ShouldReturnDataSources_ByCatalogName_WhenSuccessful() throws Exception {
    // Arrange
    ListDataSourcesRequest expectedRequest =
        ListDataSourcesRequest.newBuilder().setCatalogName(CATALOG_NAME).build();

    ListDataSourcesResponse response =
        ListDataSourcesResponse.newBuilder()
            .addDataSources(dataSourceMapper.toProto(TEST_DATA_SOURCE))
            .addDataSources(dataSourceMapper.toProto(TEST_DATA_SOURCE2))
            .build();
    when(stub.listDataSources(expectedRequest)).thenReturn(response);

    // Act
    List<DataSource> result = dataSourceClient.listDataSourcesByCatalog(CATALOG_NAME);

    // Assert
    assertThat(result).hasSize(2).containsExactly(TEST_DATA_SOURCE, TEST_DATA_SOURCE2);
    verify(stub).listDataSources(expectedRequest);
  }

  @Test
  void listDataSources_ShouldReturnEmptyList_WhenNoDataSourcesByCatalogName() throws Exception {
    // Arrange
    ListDataSourcesRequest expectedRequest =
        ListDataSourcesRequest.newBuilder().setCatalogName(CATALOG_NAME).build();

    ListDataSourcesResponse response = ListDataSourcesResponse.newBuilder().build();
    when(stub.listDataSources(expectedRequest)).thenReturn(response);

    // Act
    List<DataSource> result = dataSourceClient.listDataSourcesByCatalog(CATALOG_NAME);

    // Assert
    assertThat(result).isEmpty();
    verify(stub).listDataSources(expectedRequest);
  }

  @Test
  void listDataSources_ByCatalogName_ShouldThrowAnalyticsException_WhenGrpcCallFails() {
    // Arrange
    ListDataSourcesRequest expectedRequest =
        ListDataSourcesRequest.newBuilder().setCatalogName(CATALOG_NAME).build();

    StatusRuntimeException exception = new StatusRuntimeException(Status.INTERNAL);
    when(stub.listDataSources(expectedRequest)).thenThrow(exception);

    // Act & Assert
    assertThatThrownBy(() -> dataSourceClient.listDataSourcesByCatalog(CATALOG_NAME))
        .isInstanceOf(AnalyticsException.class)
        .hasCause(exception)
        .extracting(e -> ((AnalyticsException) e).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.CLIENT_INTERNAL_ERROR);
  }

  @Test
  void register_ShouldReturnDataSource_WhenSuccessful() throws Exception {
    // Arrange
    RegisterDataSourceRequest request =
        new RegisterDataSourceRequest(CATALOG_NAME, DATA_SOURCE_NAME, TEST_PROVIDER, null);

    RegisterResponse response =
        RegisterResponse.newBuilder()
            .setDataSource(dataSourceMapper.toProto(TEST_DATA_SOURCE))
            .build();
    when(stub.register(any())).thenReturn(response);

    // Act
    DataSource result = dataSourceClient.register(request);

    // Assert
    assertThat(result).isEqualTo(TEST_DATA_SOURCE);
    verify(stub).register(any());
  }

  @Test
  void register_ShouldThrowAnalyticsException_WhenGrpcCallFails() {
    // Arrange
    RegisterDataSourceRequest request =
        new RegisterDataSourceRequest(CATALOG_NAME, DATA_SOURCE_NAME, TEST_PROVIDER, null);

    StatusRuntimeException exception = new StatusRuntimeException(Status.INTERNAL);
    when(stub.register(any())).thenThrow(exception);

    // Act & Assert
    assertThatThrownBy(() -> dataSourceClient.register(request))
        .isInstanceOf(AnalyticsException.class)
        .hasCause(exception)
        .extracting(e -> ((AnalyticsException) e).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.CLIENT_INTERNAL_ERROR);
  }
}
