/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest;
import com.scalar.db.analytics.grpc.generated.datasource.v1.DataSourceServiceGrpc;
import com.scalar.db.analytics.grpc.generated.datasource.v1.FindDataSourceRequest;
import com.scalar.db.analytics.grpc.generated.datasource.v1.FindDataSourceResponse;
import com.scalar.db.analytics.grpc.generated.datasource.v1.ListDataSourcesRequest;
import com.scalar.db.analytics.grpc.generated.datasource.v1.ListDataSourcesResponse;
import com.scalar.db.analytics.grpc.generated.datasource.v1.RegisterResponse;
import com.scalar.db.analytics.grpc.mapper.RequestMapper;
import com.scalar.db.analytics.grpc.mapper.datasource.DataSourceMapper;
import com.scalar.db.analytics.usecase.DataSourceUseCase;
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

class DataSourceServiceImplTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private DataSourceUseCase useCase;
  private DataSourceServiceGrpc.DataSourceServiceBlockingStub stub;
  private final DataSourceMapper dataSourceMapper = DataSourceMapper.INSTANCE;
  private final RequestMapper requestMapper = RequestMapper.INSTANCE;

  @BeforeEach
  void setUp() throws IOException {
    useCase = mock(DataSourceUseCase.class);
    DataSourceServiceImpl service = new DataSourceServiceImpl(useCase);

    String serverName = InProcessServerBuilder.generateName();
    InProcessServerBuilder.forName(serverName)
        .addService(service)
        .intercept(userIdInterceptor())
        .build()
        .start();

    Channel channel = InProcessChannelBuilder.forName(serverName).build();
    stub = DataSourceServiceGrpc.newBlockingStub(channel);
  }

  @Test
  void findDataSource_shouldReturnDataSource_whenExists() {
    String catalogName = "testCatalog";
    String dataSourceName = "testDataSource";
    DataSource dataSource =
        new DataSource(
            UUID.randomUUID(),
            UUID.randomUUID(),
            dataSourceName,
            new PostgreSql("localhost", 5432, "user", "password", "database"));
    when(useCase.findDataSource(USER_ID, catalogName, dataSourceName))
        .thenReturn(Optional.of(dataSource));

    FindDataSourceRequest request =
        FindDataSourceRequest.newBuilder()
            .setCatalogName(catalogName)
            .setDataSourceName(dataSourceName)
            .build();

    FindDataSourceResponse response = stub.findDataSource(request);

    assertThat(response.getDataSource().getName()).isEqualTo(dataSourceName);
  }

  @Test
  void listDataSources_shouldReturnListOfDataSources() {
    String catalogName = "testCatalog";
    DataSource dataSource1 =
        new DataSource(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "dataSource1",
            new PostgreSql("localhost", 5432, "user1", "password", "db1"));
    DataSource dataSource2 =
        new DataSource(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "dataSource2",
            new PostgreSql("localhost", 5432, "user2", "password", "db2"));
    when(useCase.listDataSources(USER_ID, catalogName))
        .thenReturn(Arrays.asList(dataSource1, dataSource2));

    ListDataSourcesRequest request =
        ListDataSourcesRequest.newBuilder().setCatalogName(catalogName).build();

    ListDataSourcesResponse response = stub.listDataSources(request);

    assertThat(response.getDataSourcesList())
        .hasSize(2)
        .map(dataSourceMapper::toDomain)
        .containsExactlyInAnyOrder(dataSource1, dataSource2);
  }

  @Test
  void register_shouldReturnRegisteredDataSource() {
    String catalogName = "testCatalog";
    String dataSourceName = "testDataSource";
    UUID catalogId = UUID.randomUUID();
    PostgreSql provider = new PostgreSql("localhost", 5432, "user", "password", "database");
    DataSource dataSource = DataSource.create(catalogId, dataSourceName, provider);
    when(useCase.register(any(), any())).thenReturn(dataSource);

    RegisterDataSourceRequest request =
        new RegisterDataSourceRequest(catalogName, dataSource.getName(), provider, null);

    RegisterResponse response = stub.register(requestMapper.toProtoRegisterRequest(request));

    DataSource got = dataSourceMapper.toDomain(response.getDataSource());
    assertThat(got).isEqualTo(dataSource);
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
