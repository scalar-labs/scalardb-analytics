/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.namespace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.DataSourceNamespace;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;
import com.scalar.db.analytics.grpc.generated.namespace.v1.ListNamespacesRequest;
import com.scalar.db.analytics.grpc.generated.namespace.v1.ListNamespacesResponse;
import com.scalar.db.analytics.grpc.generated.namespace.v1.NamespaceServiceGrpc;
import com.scalar.db.analytics.grpc.mapper.namespace.NamespaceMapper;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlockingGrpcNamespaceClientTest {
  // Constants for test data
  private static final String CATALOG_NAME = "test-catalog";
  private static final UUID CATALOG_ID = UUID.randomUUID();
  private static final UUID DATA_SOURCE_ID = UUID.randomUUID();
  private static final UUID NAMESPACE_ID = UUID.randomUUID();
  private static final String DATA_SOURCE_NAME = "test-datasource";
  private static final List<String> NAMESPACE_NAMES = Arrays.asList("schema1", "schema2");

  // Shared model instances for testing
  private static final DataSource TEST_DATA_SOURCE =
      new DataSource(
          DATA_SOURCE_ID,
          CATALOG_ID,
          DATA_SOURCE_NAME,
          PostgreSql.builder()
              .host("localhost")
              .port(5432)
              .username("user")
              .password("password")
              .database("db")
              .build());
  private static final Namespace TEST_NAMESPACE =
      new Namespace(NAMESPACE_ID, DATA_SOURCE_ID, NAMESPACE_NAMES);
  private static final DataSourceNamespace TEST_DATA_SOURCE_NAMESPACE =
      new DataSourceNamespace(TEST_DATA_SOURCE, TEST_NAMESPACE);

  // Mock the gRPC blocking stub
  @Mock private NamespaceServiceGrpc.NamespaceServiceBlockingStub stub;

  // The test target
  private NamespaceClient namespaceClient;
  private final NamespaceMapper namespaceMapper = NamespaceMapper.INSTANCE;

  @BeforeEach
  void setUp() {
    namespaceClient = new BlockingGrpcNamespaceClient(stub);
  }

  @Test
  void listNamespaces_ShouldReturnNamespaces_WhenNamespacesByCatalogNameExist() throws Exception {
    // Arrange
    ListNamespacesRequest expectedRequest =
        ListNamespacesRequest.newBuilder().setCatalogName(CATALOG_NAME).build();

    com.scalar.db.analytics.grpc.generated.namespace.v1.DataSourceNamespace
        dataSourceNamespaceProto = namespaceMapper.toProto(TEST_DATA_SOURCE_NAMESPACE);

    ListNamespacesResponse response =
        ListNamespacesResponse.newBuilder().addNamespaces(dataSourceNamespaceProto).build();
    when(stub.listNamespaces(expectedRequest)).thenReturn(response);

    // Act
    List<DataSourceNamespace> result = namespaceClient.listNamespacesByCatalog(CATALOG_NAME);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(TEST_DATA_SOURCE_NAMESPACE);
    verify(stub).listNamespaces(expectedRequest);
  }

  @Test
  void listNamespaces_ShouldReturnEmptyList_WhenNoNamespacesByCatalogNameExist() throws Exception {
    // Arrange
    ListNamespacesRequest expectedRequest =
        ListNamespacesRequest.newBuilder().setCatalogName(CATALOG_NAME).build();

    ListNamespacesResponse response = ListNamespacesResponse.newBuilder().build();
    when(stub.listNamespaces(expectedRequest)).thenReturn(response);

    // Act
    List<DataSourceNamespace> result = namespaceClient.listNamespacesByCatalog(CATALOG_NAME);

    // Assert
    assertThat(result).isEmpty();
    verify(stub).listNamespaces(expectedRequest);
  }

  @Test
  void listNamespaces_ByCatalogName_ShouldThrowAnalyticsException_WhenGrpcCallFails() {
    // Arrange
    ListNamespacesRequest expectedRequest =
        ListNamespacesRequest.newBuilder().setCatalogName(CATALOG_NAME).build();

    StatusRuntimeException exception = new StatusRuntimeException(Status.INTERNAL);
    when(stub.listNamespaces(expectedRequest)).thenThrow(exception);

    // Act & Assert
    assertThatThrownBy(() -> namespaceClient.listNamespacesByCatalog(CATALOG_NAME))
        .isInstanceOf(AnalyticsException.class)
        .hasCause(exception)
        .extracting(e -> ((AnalyticsException) e).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.CLIENT_INTERNAL_ERROR);
  }
}
