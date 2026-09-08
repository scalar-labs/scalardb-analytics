/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.grpc.generated.datasource.v1.DataSourceServiceGrpc.DataSourceServiceImplBase;
import com.scalar.db.analytics.grpc.generated.datasource.v1.DeleteDataSourceByIdRequest;
import com.scalar.db.analytics.grpc.generated.datasource.v1.DeleteDataSourceByIdResponse;
import com.scalar.db.analytics.grpc.generated.datasource.v1.DeleteDataSourceRequest;
import com.scalar.db.analytics.grpc.generated.datasource.v1.DeleteDataSourceResponse;
import com.scalar.db.analytics.grpc.generated.datasource.v1.FindDataSourceByIdRequest;
import com.scalar.db.analytics.grpc.generated.datasource.v1.FindDataSourceByIdResponse;
import com.scalar.db.analytics.grpc.generated.datasource.v1.FindDataSourceRequest;
import com.scalar.db.analytics.grpc.generated.datasource.v1.FindDataSourceResponse;
import com.scalar.db.analytics.grpc.generated.datasource.v1.ListDataSourcesRequest;
import com.scalar.db.analytics.grpc.generated.datasource.v1.ListDataSourcesResponse;
import com.scalar.db.analytics.grpc.generated.datasource.v1.RegisterRequest;
import com.scalar.db.analytics.grpc.generated.datasource.v1.RegisterResponse;
import com.scalar.db.analytics.grpc.mapper.RequestMapper;
import com.scalar.db.analytics.grpc.mapper.datasource.DataSourceMapper;
import com.scalar.db.analytics.usecase.DataSourceUseCase;
import io.grpc.stub.StreamObserver;
import java.util.Objects;
import java.util.UUID;

public class DataSourceServiceImpl extends DataSourceServiceImplBase {
  private final DataSourceUseCase useCase;

  public DataSourceServiceImpl(DataSourceUseCase useCase) {
    this.useCase = useCase;
  }

  @Override
  public void findDataSource(
      FindDataSourceRequest request, StreamObserver<FindDataSourceResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    FindDataSourceResponse.Builder builder = FindDataSourceResponse.newBuilder();
    useCase
        .findDataSource(userId, request.getCatalogName(), request.getDataSourceName())
        .ifPresent(
            dataSource -> builder.setDataSource(DataSourceMapper.INSTANCE.toProto(dataSource)));

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void listDataSources(
      ListDataSourcesRequest request, StreamObserver<ListDataSourcesResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    ListDataSourcesResponse.Builder builder = ListDataSourcesResponse.newBuilder();

    useCase.listDataSources(userId, request.getCatalogName()).stream()
        .map(DataSourceMapper.INSTANCE::toProto)
        .forEach(builder::addDataSources);

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    // The mapper returns null only for a null request, and gRPC never passes one.
    DataSource register =
        useCase.register(
            userId,
            Objects.requireNonNull(
                RequestMapper.INSTANCE.toDomainRegisterDataSourceRequest(request)));

    RegisterResponse response =
        RegisterResponse.newBuilder()
            .setDataSource(DataSourceMapper.INSTANCE.toProto(register))
            .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void findDataSourceById(
      FindDataSourceByIdRequest request,
      StreamObserver<FindDataSourceByIdResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    FindDataSourceByIdResponse.Builder builder = FindDataSourceByIdResponse.newBuilder();
    useCase
        .describeDataSourceById(userId, UUID.fromString(request.getDataSourceId()))
        .ifPresent(
            dataSource -> builder.setDataSource(DataSourceMapper.INSTANCE.toProto(dataSource)));

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void deleteDataSource(
      DeleteDataSourceRequest request, StreamObserver<DeleteDataSourceResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    boolean deleted =
        useCase.deleteDataSource(
            userId, request.getCatalogName(), request.getDataSourceName(), request.getCascade());

    DeleteDataSourceResponse response =
        DeleteDataSourceResponse.newBuilder().setDeleted(deleted).build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void deleteDataSourceById(
      DeleteDataSourceByIdRequest request,
      StreamObserver<DeleteDataSourceByIdResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    boolean deleted =
        useCase.deleteDataSourceById(
            userId, UUID.fromString(request.getDataSourceId()), request.getCascade());

    DeleteDataSourceByIdResponse response =
        DeleteDataSourceByIdResponse.newBuilder().setDeleted(deleted).build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
