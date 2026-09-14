/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.grpc.generated.catalog.v1.CatalogServiceGrpc;
import com.scalar.db.analytics.grpc.generated.catalog.v1.CreateCatalogRequest;
import com.scalar.db.analytics.grpc.generated.catalog.v1.CreateCatalogResponse;
import com.scalar.db.analytics.grpc.generated.catalog.v1.FindCatalogRequest;
import com.scalar.db.analytics.grpc.generated.catalog.v1.FindCatalogResponse;
import com.scalar.db.analytics.grpc.generated.catalog.v1.ListAllCatalogsRequest;
import com.scalar.db.analytics.grpc.generated.catalog.v1.ListAllCatalogsResponse;
import com.scalar.db.analytics.grpc.mapper.catalog.CatalogMapper;
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
class BlockingGrpcCatalogClientTest {
  private static final String CATALOG_NAME = "test-catalog";
  private static final String CATALOG2_NAME = "catalog2";
  private static final UUID CATALOG_ID = UUID.randomUUID();
  private static final UUID CATALOG2_ID = UUID.randomUUID();

  private static final Catalog TEST_CATALOG = Catalog.of(CATALOG_ID, CATALOG_NAME);
  private static final Catalog TEST_CATALOG2 = Catalog.of(CATALOG2_ID, CATALOG2_NAME);

  @Mock private CatalogServiceGrpc.CatalogServiceBlockingStub stub;

  private CatalogClient catalogClient;
  private final CatalogMapper catalogMapper = CatalogMapper.INSTANCE;

  @BeforeEach
  void setUp() {
    catalogClient = new BlockingGrpcCatalogClient(stub);
  }

  @Test
  void createCatalog_ShouldReturnCatalog_WhenSuccessful() throws Exception {
    // Arrange
    CreateCatalogRequest expectedRequest =
        CreateCatalogRequest.newBuilder().setCatalogName(CATALOG_NAME).build();

    com.scalar.db.analytics.grpc.generated.catalog.v1.Catalog catalogProto =
        catalogMapper.toProto(TEST_CATALOG);

    CreateCatalogResponse response =
        CreateCatalogResponse.newBuilder().setCatalog(catalogProto).build();
    when(stub.createCatalog(expectedRequest)).thenReturn(response);

    // Act
    Catalog result = catalogClient.createCatalog(CATALOG_NAME);

    // Assert
    assertThat(result).isEqualTo(TEST_CATALOG);
    verify(stub).createCatalog(expectedRequest);
  }

  @Test
  void createCatalog_ShouldThrowAnalyticsException_WhenGrpcCallFails() {
    // Arrange
    CreateCatalogRequest expectedRequest =
        CreateCatalogRequest.newBuilder().setCatalogName(CATALOG_NAME).build();
    StatusRuntimeException exception = new StatusRuntimeException(Status.INTERNAL);
    when(stub.createCatalog(expectedRequest)).thenThrow(exception);

    // Act & Assert
    assertThatThrownBy(() -> catalogClient.createCatalog(CATALOG_NAME))
        .isInstanceOf(AnalyticsException.class)
        .hasCause(exception)
        .extracting(e -> ((AnalyticsException) e).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.CLIENT_INTERNAL_ERROR);
  }

  @Test
  void findCatalog_ShouldReturnCatalog_WhenCatalogByNameExists() throws Exception {
    // Arrange
    FindCatalogRequest expectedRequest =
        FindCatalogRequest.newBuilder().setCatalogName(CATALOG_NAME).build();

    com.scalar.db.analytics.grpc.generated.catalog.v1.Catalog catalogProto =
        catalogMapper.toProto(TEST_CATALOG);

    FindCatalogResponse response =
        FindCatalogResponse.newBuilder().setCatalog(catalogProto).build();
    when(stub.findCatalog(expectedRequest)).thenReturn(response);

    // Act
    Optional<Catalog> result = catalogClient.findCatalogByName(CATALOG_NAME);

    // Assert
    assertThat(result).isPresent().hasValue(TEST_CATALOG);
    verify(stub).findCatalog(expectedRequest);
  }

  @Test
  void findCatalog_ShouldReturnEmpty_WhenCatalogByNameDoesNotExist() throws Exception {
    // Arrange
    FindCatalogRequest expectedRequest =
        FindCatalogRequest.newBuilder().setCatalogName(CATALOG_NAME).build();
    FindCatalogResponse response = FindCatalogResponse.newBuilder().build();
    when(stub.findCatalog(expectedRequest)).thenReturn(response);

    // Act
    Optional<Catalog> result = catalogClient.findCatalogByName(CATALOG_NAME);

    // Assert
    assertThat(result).isEmpty();
    verify(stub).findCatalog(expectedRequest);
  }

  @Test
  void findCatalog_ByName_ShouldThrowAnalyticsException_WhenGrpcCallFails() {
    // Arrange
    FindCatalogRequest expectedRequest =
        FindCatalogRequest.newBuilder().setCatalogName(CATALOG_NAME).build();
    StatusRuntimeException exception = new StatusRuntimeException(Status.INTERNAL);
    when(stub.findCatalog(expectedRequest)).thenThrow(exception);

    // Act & Assert
    assertThatThrownBy(() -> catalogClient.findCatalogByName(CATALOG_NAME))
        .isInstanceOf(AnalyticsException.class)
        .hasCause(exception)
        .extracting(e -> ((AnalyticsException) e).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.CLIENT_INTERNAL_ERROR);
  }

  @Test
  void listAllCatalogs_ShouldReturnCatalogs_WhenSuccessful() throws Exception {
    // Arrange
    ListAllCatalogsRequest expectedRequest = ListAllCatalogsRequest.getDefaultInstance();

    ListAllCatalogsResponse response =
        ListAllCatalogsResponse.newBuilder()
            .addCatalogs(catalogMapper.toProto(TEST_CATALOG))
            .addCatalogs(catalogMapper.toProto(TEST_CATALOG2))
            .build();
    when(stub.listAllCatalogs(expectedRequest)).thenReturn(response);

    // Act
    List<Catalog> result = catalogClient.listAllCatalogs();

    // Assert
    assertThat(result).hasSize(2).containsExactly(TEST_CATALOG, TEST_CATALOG2);
    verify(stub).listAllCatalogs(expectedRequest);
  }

  @Test
  void listAllCatalogs_ShouldReturnEmptyList_WhenNoCatalogs() throws Exception {
    // Arrange
    ListAllCatalogsRequest expectedRequest = ListAllCatalogsRequest.getDefaultInstance();
    ListAllCatalogsResponse response = ListAllCatalogsResponse.newBuilder().build();
    when(stub.listAllCatalogs(expectedRequest)).thenReturn(response);

    // Act
    List<Catalog> result = catalogClient.listAllCatalogs();

    // Assert
    assertThat(result).isEmpty();
    verify(stub).listAllCatalogs(expectedRequest);
  }

  @Test
  void listAllCatalogs_ShouldThrowAnalyticsException_WhenGrpcCallFails() {
    // Arrange
    ListAllCatalogsRequest expectedRequest = ListAllCatalogsRequest.getDefaultInstance();
    StatusRuntimeException exception = new StatusRuntimeException(Status.INTERNAL);
    when(stub.listAllCatalogs(expectedRequest)).thenThrow(exception);

    // Act & Assert
    assertThatThrownBy(() -> catalogClient.listAllCatalogs())
        .isInstanceOf(AnalyticsException.class)
        .hasCause(exception)
        .extracting(e -> ((AnalyticsException) e).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.CLIENT_INTERNAL_ERROR);
  }
}
