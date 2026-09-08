/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.namespace;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.scalar.db.analytics.api.model.DataSourceNamespace;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.grpc.generated.namespace.v1.DescribeNamespaceByIdRequest;
import com.scalar.db.analytics.grpc.generated.namespace.v1.DescribeNamespaceByIdResponse;
import com.scalar.db.analytics.grpc.generated.namespace.v1.DescribeNamespaceRequest;
import com.scalar.db.analytics.grpc.generated.namespace.v1.DescribeNamespaceResponse;
import com.scalar.db.analytics.grpc.generated.namespace.v1.ListNamespacesRequest;
import com.scalar.db.analytics.grpc.generated.namespace.v1.NamespaceServiceGrpc;
import com.scalar.db.analytics.grpc.mapper.namespace.NamespaceMapper;
import com.scalar.db.analytics.sdk.exception.GrpcExceptionMapper;
import io.grpc.Channel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link NamespaceClient}.
 *
 * <p>This class is for internal SDK use only and should not be instantiated directly by SDK users.
 */
public class BlockingGrpcNamespaceClient implements NamespaceClient {
  private final NamespaceServiceGrpc.NamespaceServiceBlockingStub stub;

  public BlockingGrpcNamespaceClient(Channel channel) {
    this.stub = NamespaceServiceGrpc.newBlockingStub(channel);
  }

  @VisibleForTesting
  BlockingGrpcNamespaceClient(NamespaceServiceGrpc.NamespaceServiceBlockingStub stub) {
    this.stub = stub;
  }

  @Override
  public List<DataSourceNamespace> listNamespacesByCatalog(String catalogName) {
    try {
      ListNamespacesRequest request =
          ListNamespacesRequest.newBuilder().setCatalogName(catalogName).build();

      return stub.listNamespaces(request).getNamespacesList().stream()
          .map(NamespaceMapper.INSTANCE::toDomain)
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "listNamespacesByCatalog", ImmutableMap.of("catalogName", catalogName));
    }
  }

  @Override
  public Optional<Namespace> findNamespaceByName(
      String catalogName, String dataSourceName, List<String> namespaceNames) {
    try {
      DescribeNamespaceRequest request =
          DescribeNamespaceRequest.newBuilder()
              .setCatalogName(catalogName)
              .setDataSourceName(dataSourceName)
              .addAllNamespaceNames(namespaceNames)
              .build();
      DescribeNamespaceResponse response = stub.describeNamespace(request);
      return response.hasNamespace()
          ? Optional.of(NamespaceMapper.INSTANCE.toDomain(response.getNamespace()))
          : Optional.empty();
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e,
          "findNamespaceByName",
          ImmutableMap.of("catalogName", catalogName, "dataSourceName", dataSourceName));
    }
  }

  @Override
  public Optional<Namespace> findNamespaceById(UUID namespaceId) {
    try {
      DescribeNamespaceByIdRequest request =
          DescribeNamespaceByIdRequest.newBuilder().setNamespaceId(namespaceId.toString()).build();
      DescribeNamespaceByIdResponse response = stub.describeNamespaceById(request);
      return response.hasNamespace()
          ? Optional.of(NamespaceMapper.INSTANCE.toDomain(response.getNamespace()))
          : Optional.empty();
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "findNamespaceById", ImmutableMap.of("namespaceId", namespaceId.toString()));
    }
  }
}
