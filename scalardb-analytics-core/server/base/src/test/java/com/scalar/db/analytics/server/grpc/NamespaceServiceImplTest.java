/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.DataSourceNamespace;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;
import com.scalar.db.analytics.grpc.generated.namespace.v1.ListNamespacesRequest;
import com.scalar.db.analytics.grpc.generated.namespace.v1.ListNamespacesResponse;
import com.scalar.db.analytics.grpc.generated.namespace.v1.NamespaceServiceGrpc;
import com.scalar.db.analytics.grpc.generated.namespace.v1.NamespaceServiceGrpc.NamespaceServiceBlockingStub;
import com.scalar.db.analytics.grpc.mapper.namespace.NamespaceMapper;
import com.scalar.db.analytics.usecase.NamespaceUseCase;
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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NamespaceServiceImplTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private NamespaceUseCase useCase;
  private NamespaceServiceBlockingStub stub;
  private final NamespaceMapper namespaceMapper = NamespaceMapper.INSTANCE;

  private final DataSource testDataSource =
      DataSource.create(
          UUID.randomUUID(),
          "dataSource1",
          PostgreSql.builder()
              .host("localhost")
              .port(5432)
              .username("user")
              .password("password")
              .database("db")
              .build());

  @BeforeEach
  void setUp() throws IOException {
    useCase = mock(NamespaceUseCase.class);
    NamespaceServiceImpl service = new NamespaceServiceImpl(useCase);

    String serverName = InProcessServerBuilder.generateName();
    InProcessServerBuilder.forName(serverName)
        .addService(service)
        .intercept(userIdInterceptor())
        .build()
        .start();

    Channel channel = InProcessChannelBuilder.forName(serverName).build();
    stub = NamespaceServiceGrpc.newBlockingStub(channel);
  }

  @Test
  void listNamespaces_ShouldReturnNamespaces_WhenUseCaseReturnsNamespaces() {
    String catalogName = "test_catalog";
    DataSourceNamespace namespace1 =
        new DataSourceNamespace(
            testDataSource,
            Namespace.create(UUID.randomUUID(), Collections.singletonList("namespace1")));
    DataSourceNamespace namespace2 =
        new DataSourceNamespace(
            testDataSource,
            Namespace.create(UUID.randomUUID(), Collections.singletonList("namespace2")));
    when(useCase.listNamespaces(USER_ID, catalogName))
        .thenReturn(Arrays.asList(namespace1, namespace2));

    ListNamespacesRequest request =
        ListNamespacesRequest.newBuilder().setCatalogName(catalogName).build();

    ListNamespacesResponse response = stub.listNamespaces(request);

    assertThat(response.getNamespacesList())
        .hasSize(2)
        .map(namespaceMapper::toDomain)
        .containsExactlyInAnyOrder(namespace1, namespace2);
  }

  @Test
  void listNamespaces_ShouldHandleEmptyNamespaces_WhenUseCaseReturnsEmptyList() {
    String catalogName = "test_catalog";
    when(useCase.listNamespaces(USER_ID, catalogName)).thenReturn(Collections.emptyList());

    ListNamespacesRequest request =
        ListNamespacesRequest.newBuilder().setCatalogName(catalogName).build();

    ListNamespacesResponse response = stub.listNamespaces(request);

    assertThat(response.getNamespacesList()).isEmpty();
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
