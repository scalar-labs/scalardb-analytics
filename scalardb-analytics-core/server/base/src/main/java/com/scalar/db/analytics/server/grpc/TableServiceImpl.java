/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import com.scalar.db.analytics.grpc.generated.table.v1.DescribeTableByIdRequest;
import com.scalar.db.analytics.grpc.generated.table.v1.DescribeTableByIdResponse;
import com.scalar.db.analytics.grpc.generated.table.v1.DescribeTableRequest;
import com.scalar.db.analytics.grpc.generated.table.v1.DescribeTableResponse;
import com.scalar.db.analytics.grpc.generated.table.v1.ListTablesByNamespaceRequest;
import com.scalar.db.analytics.grpc.generated.table.v1.ListTablesByNamespaceResponse;
import com.scalar.db.analytics.grpc.generated.table.v1.ListTablesRequest;
import com.scalar.db.analytics.grpc.generated.table.v1.ListTablesResponse;
import com.scalar.db.analytics.grpc.generated.table.v1.TableServiceGrpc.TableServiceImplBase;
import com.scalar.db.analytics.grpc.mapper.table.TableMapper;
import com.scalar.db.analytics.usecase.TableUseCase;
import io.grpc.stub.StreamObserver;
import java.util.UUID;

public class TableServiceImpl extends TableServiceImplBase {
  private final TableUseCase useCase;
  private final TableMapper tableMapper = TableMapper.INSTANCE;

  public TableServiceImpl(TableUseCase useCase) {
    this.useCase = useCase;
  }

  @Override
  public void listTables(
      ListTablesRequest request, StreamObserver<ListTablesResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    ListTablesResponse.Builder builder = ListTablesResponse.newBuilder();

    useCase.listTables(userId, request.getCatalogName()).stream()
        .map(tableMapper::toProto)
        .forEach(builder::addTables);

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void listTablesByNamespace(
      ListTablesByNamespaceRequest request,
      StreamObserver<ListTablesByNamespaceResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    ListTablesByNamespaceResponse.Builder builder = ListTablesByNamespaceResponse.newBuilder();

    useCase
        .listTablesByNamespace(
            userId,
            request.getCatalogName(),
            request.getDataSourceName(),
            request.getNamespaceNamesList())
        .stream()
        .map(tableMapper::toProto)
        .forEach(builder::addTables);

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void describeTable(
      DescribeTableRequest request, StreamObserver<DescribeTableResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    DescribeTableResponse.Builder builder = DescribeTableResponse.newBuilder();

    useCase
        .describeTable(
            userId,
            request.getCatalogName(),
            request.getDataSourceName(),
            request.getNamespaceNamesList(),
            request.getTableName())
        .ifPresent(table -> builder.setTable(tableMapper.toProto(table)));

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void describeTableById(
      DescribeTableByIdRequest request,
      StreamObserver<DescribeTableByIdResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    DescribeTableByIdResponse.Builder builder = DescribeTableByIdResponse.newBuilder();

    useCase
        .describeTableById(userId, UUID.fromString(request.getTableId()))
        .ifPresent(table -> builder.setTable(tableMapper.toProto(table)));

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }
}
