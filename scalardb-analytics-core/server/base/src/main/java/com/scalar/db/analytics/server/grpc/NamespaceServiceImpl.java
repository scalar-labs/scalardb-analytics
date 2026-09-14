/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import com.scalar.db.analytics.grpc.generated.namespace.v1.DescribeNamespaceByIdRequest;
import com.scalar.db.analytics.grpc.generated.namespace.v1.DescribeNamespaceByIdResponse;
import com.scalar.db.analytics.grpc.generated.namespace.v1.DescribeNamespaceRequest;
import com.scalar.db.analytics.grpc.generated.namespace.v1.DescribeNamespaceResponse;
import com.scalar.db.analytics.grpc.generated.namespace.v1.ListNamespacesRequest;
import com.scalar.db.analytics.grpc.generated.namespace.v1.ListNamespacesResponse;
import com.scalar.db.analytics.grpc.generated.namespace.v1.NamespaceServiceGrpc.NamespaceServiceImplBase;
import com.scalar.db.analytics.grpc.mapper.namespace.NamespaceMapper;
import com.scalar.db.analytics.usecase.NamespaceUseCase;
import io.grpc.stub.StreamObserver;
import java.util.UUID;

public class NamespaceServiceImpl extends NamespaceServiceImplBase {
  private final NamespaceUseCase useCase;
  private final NamespaceMapper namespaceMapper = NamespaceMapper.INSTANCE;

  public NamespaceServiceImpl(NamespaceUseCase useCase) {
    this.useCase = useCase;
  }

  @Override
  public void listNamespaces(
      ListNamespacesRequest request, StreamObserver<ListNamespacesResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    ListNamespacesResponse.Builder builder = ListNamespacesResponse.newBuilder();

    useCase.listNamespaces(userId, request.getCatalogName()).stream()
        .map(namespaceMapper::toProto)
        .forEach(builder::addNamespaces);

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void describeNamespace(
      DescribeNamespaceRequest request,
      StreamObserver<DescribeNamespaceResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    DescribeNamespaceResponse.Builder builder = DescribeNamespaceResponse.newBuilder();

    useCase
        .describeNamespace(
            userId,
            request.getCatalogName(),
            request.getDataSourceName(),
            request.getNamespaceNamesList())
        .ifPresent(namespace -> builder.setNamespace(namespaceMapper.toProto(namespace)));

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void describeNamespaceById(
      DescribeNamespaceByIdRequest request,
      StreamObserver<DescribeNamespaceByIdResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    DescribeNamespaceByIdResponse.Builder builder = DescribeNamespaceByIdResponse.newBuilder();

    useCase
        .describeNamespaceById(userId, UUID.fromString(request.getNamespaceId()))
        .ifPresent(namespace -> builder.setNamespace(namespaceMapper.toProto(namespace)));

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }
}
