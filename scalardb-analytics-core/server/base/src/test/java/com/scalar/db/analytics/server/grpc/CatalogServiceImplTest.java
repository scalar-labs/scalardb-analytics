/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.grpc.generated.catalog.v1.CatalogServiceGrpc;
import com.scalar.db.analytics.grpc.generated.catalog.v1.CreateCatalogRequest;
import com.scalar.db.analytics.grpc.generated.catalog.v1.CreateCatalogResponse;
import com.scalar.db.analytics.grpc.generated.catalog.v1.FindCatalogRequest;
import com.scalar.db.analytics.grpc.generated.catalog.v1.FindCatalogResponse;
import com.scalar.db.analytics.grpc.generated.catalog.v1.InitializeCatalogRequest;
import com.scalar.db.analytics.grpc.generated.catalog.v1.InitializeCatalogResponse;
import com.scalar.db.analytics.grpc.generated.catalog.v1.ListAllCatalogsRequest;
import com.scalar.db.analytics.grpc.generated.catalog.v1.ListAllCatalogsResponse;
import com.scalar.db.analytics.grpc.mapper.catalog.CatalogMapper;
import com.scalar.db.analytics.usecase.CatalogUseCase;
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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CatalogServiceImplTest {
  private static final UUID USER_ID = UUID.randomUUID();
  private CatalogUseCase useCase;
  private CatalogServiceGrpc.CatalogServiceBlockingStub stub;
  private final CatalogMapper catalogMapper = CatalogMapper.INSTANCE;

  @BeforeEach
  void setUp() throws IOException {
    useCase = mock(CatalogUseCase.class);
    CatalogServiceImpl service = new CatalogServiceImpl(useCase);

    String serverName = InProcessServerBuilder.generateName();
    InProcessServerBuilder.forName(serverName)
        .addService(service)
        .intercept(userIdInterceptor())
        .build()
        .start();

    Channel channel = InProcessChannelBuilder.forName(serverName).build();
    stub = CatalogServiceGrpc.newBlockingStub(channel);
  }

  @Test
  void createCatalog_shouldReturnCreatedCatalog() {
    String catalogName = "test_catalog";
    Catalog catalog = Catalog.create(catalogName);
    when(useCase.createCatalog(USER_ID, catalogName)).thenReturn(catalog);

    CreateCatalogRequest request =
        CreateCatalogRequest.newBuilder().setCatalogName(catalogName).build();

    CreateCatalogResponse response = stub.createCatalog(request);

    Catalog got = catalogMapper.toDomain(response.getCatalog());
    assertThat(got).isEqualTo(catalog);
  }

  @Test
  void findCatalog_shouldReturnCatalogIfExists() {
    String catalogName = "test_catalog";
    Catalog catalog = Catalog.create(catalogName);
    when(useCase.findCatalog(USER_ID, catalogName)).thenReturn(Optional.of(catalog));

    FindCatalogRequest request =
        FindCatalogRequest.newBuilder().setCatalogName(catalogName).build();

    FindCatalogResponse response = stub.findCatalog(request);

    assertThat(response.hasCatalog()).isTrue();
    Catalog got = catalogMapper.toDomain(response.getCatalog());
    assertThat(got).isEqualTo(catalog);
  }

  @Test
  void listAllCatalogs_shouldReturnAllCatalogs() {
    Catalog catalog1 = Catalog.create("catalog1");
    Catalog catalog2 = Catalog.create("catalog2");
    when(useCase.listAllCatalogs(USER_ID)).thenReturn(Arrays.asList(catalog1, catalog2));

    ListAllCatalogsRequest request = ListAllCatalogsRequest.newBuilder().build();

    ListAllCatalogsResponse response = stub.listAllCatalogs(request);

    assertThat(response.getCatalogsCount()).isEqualTo(2);
    Catalog got1 = catalogMapper.toDomain(response.getCatalogs(0));
    Catalog got2 = catalogMapper.toDomain(response.getCatalogs(1));
    assertThat(got1).isEqualTo(catalog1);
    assertThat(got2).isEqualTo(catalog2);
  }

  @Test
  void initializeCatalog_shouldReturnInitializedCatalog() {
    String catalogName = "test_catalog";
    Catalog catalog = Catalog.create(catalogName);
    when(useCase.initializeCatalog(eq(USER_ID), eq(catalogName), anyList()))
        .thenReturn(Optional.of(catalog));

    InitializeCatalogRequest request =
        InitializeCatalogRequest.newBuilder().setCatalogName(catalogName).build();

    InitializeCatalogResponse response = stub.initializeCatalog(request);

    assertThat(response.hasCatalog()).isTrue();
    Catalog got = catalogMapper.toDomain(response.getCatalog());
    assertThat(got).isEqualTo(catalog);
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
