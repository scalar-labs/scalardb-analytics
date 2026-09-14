/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.grpc.generated.catalog.v1.CatalogServiceGrpc.CatalogServiceImplBase;
import com.scalar.db.analytics.grpc.generated.catalog.v1.CreateCatalogRequest;
import com.scalar.db.analytics.grpc.generated.catalog.v1.CreateCatalogResponse;
import com.scalar.db.analytics.grpc.generated.catalog.v1.DeleteCatalogByIdRequest;
import com.scalar.db.analytics.grpc.generated.catalog.v1.DeleteCatalogByIdResponse;
import com.scalar.db.analytics.grpc.generated.catalog.v1.DeleteCatalogRequest;
import com.scalar.db.analytics.grpc.generated.catalog.v1.DeleteCatalogResponse;
import com.scalar.db.analytics.grpc.generated.catalog.v1.FindCatalogByIdRequest;
import com.scalar.db.analytics.grpc.generated.catalog.v1.FindCatalogByIdResponse;
import com.scalar.db.analytics.grpc.generated.catalog.v1.FindCatalogRequest;
import com.scalar.db.analytics.grpc.generated.catalog.v1.FindCatalogResponse;
import com.scalar.db.analytics.grpc.generated.catalog.v1.InitializeCatalogRequest;
import com.scalar.db.analytics.grpc.generated.catalog.v1.InitializeCatalogResponse;
import com.scalar.db.analytics.grpc.generated.catalog.v1.ListAllCatalogsRequest;
import com.scalar.db.analytics.grpc.generated.catalog.v1.ListAllCatalogsResponse;
import com.scalar.db.analytics.grpc.mapper.RequestMapper;
import com.scalar.db.analytics.grpc.mapper.catalog.CatalogMapper;
import com.scalar.db.analytics.usecase.CatalogUseCase;
import java.util.UUID;
import java.util.stream.Collectors;

public class CatalogServiceImpl extends CatalogServiceImplBase {
  private final CatalogUseCase catalogUseCase;

  public CatalogServiceImpl(CatalogUseCase catalogUseCase) {
    this.catalogUseCase = catalogUseCase;
  }

  @Override
  public void createCatalog(
      CreateCatalogRequest request,
      io.grpc.stub.StreamObserver<CreateCatalogResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    Catalog catalog = catalogUseCase.createCatalog(userId, request.getCatalogName());

    CreateCatalogResponse response =
        CreateCatalogResponse.newBuilder()
            .setCatalog(CatalogMapper.INSTANCE.toProto(catalog))
            .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void findCatalog(
      FindCatalogRequest request,
      io.grpc.stub.StreamObserver<FindCatalogResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    FindCatalogResponse.Builder responseBuilder = FindCatalogResponse.newBuilder();

    catalogUseCase
        .findCatalog(userId, request.getCatalogName())
        .ifPresent(value -> responseBuilder.setCatalog(CatalogMapper.INSTANCE.toProto(value)));

    responseObserver.onNext(responseBuilder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void listAllCatalogs(
      ListAllCatalogsRequest request,
      io.grpc.stub.StreamObserver<ListAllCatalogsResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    ListAllCatalogsResponse.Builder builder = ListAllCatalogsResponse.newBuilder();

    catalogUseCase.listAllCatalogs(userId).stream()
        .map(CatalogMapper.INSTANCE::toProto)
        .forEach(builder::addCatalogs);

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void initializeCatalog(
      InitializeCatalogRequest request,
      io.grpc.stub.StreamObserver<InitializeCatalogResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    InitializeCatalogResponse.Builder responseBuilder = InitializeCatalogResponse.newBuilder();

    catalogUseCase
        .initializeCatalog(
            userId,
            request.getCatalogName(),
            request.getDataSourcesList().stream()
                .map(RequestMapper.INSTANCE::toDomainRegisterDataSourceRequest)
                .collect(Collectors.toList()))
        .ifPresent(value -> responseBuilder.setCatalog(CatalogMapper.INSTANCE.toProto(value)));

    responseObserver.onNext(responseBuilder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void findCatalogById(
      FindCatalogByIdRequest request,
      io.grpc.stub.StreamObserver<FindCatalogByIdResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    FindCatalogByIdResponse.Builder responseBuilder = FindCatalogByIdResponse.newBuilder();

    catalogUseCase
        .describeCatalogById(userId, UUID.fromString(request.getCatalogId()))
        .ifPresent(value -> responseBuilder.setCatalog(CatalogMapper.INSTANCE.toProto(value)));

    responseObserver.onNext(responseBuilder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void deleteCatalog(
      DeleteCatalogRequest request,
      io.grpc.stub.StreamObserver<DeleteCatalogResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    boolean deleted =
        catalogUseCase.deleteCatalog(userId, request.getCatalogName(), request.getCascade());

    DeleteCatalogResponse response = DeleteCatalogResponse.newBuilder().setDeleted(deleted).build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void deleteCatalogById(
      DeleteCatalogByIdRequest request,
      io.grpc.stub.StreamObserver<DeleteCatalogByIdResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    boolean deleted =
        catalogUseCase.deleteCatalogById(
            userId, UUID.fromString(request.getCatalogId()), request.getCascade());

    DeleteCatalogByIdResponse response =
        DeleteCatalogByIdResponse.newBuilder().setDeleted(deleted).build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
