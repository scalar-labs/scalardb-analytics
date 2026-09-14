/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.catalog;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.grpc.generated.catalog.v1.CatalogServiceGrpc;
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
import com.scalar.db.analytics.grpc.generated.catalog.v1.ListAllCatalogsRequest;
import com.scalar.db.analytics.grpc.generated.catalog.v1.ListAllCatalogsResponse;
import com.scalar.db.analytics.grpc.mapper.catalog.CatalogMapper;
import com.scalar.db.analytics.sdk.exception.GrpcExceptionMapper;
import io.grpc.Channel;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link CatalogClient}.
 *
 * <p>This class is for internal SDK use only and should not be instantiated directly by SDK users.
 */
public class BlockingGrpcCatalogClient implements CatalogClient {
  private final CatalogServiceGrpc.CatalogServiceBlockingStub stub;

  public BlockingGrpcCatalogClient(Channel channel) {
    this.stub = CatalogServiceGrpc.newBlockingStub(channel);
  }

  @VisibleForTesting
  BlockingGrpcCatalogClient(CatalogServiceGrpc.CatalogServiceBlockingStub stub) {
    this.stub = stub;
  }

  @Override
  public Catalog createCatalog(String catalogName) {
    try {
      CreateCatalogRequest request =
          CreateCatalogRequest.newBuilder().setCatalogName(catalogName).build();
      CreateCatalogResponse response = stub.createCatalog(request);
      return Objects.requireNonNull(CatalogMapper.INSTANCE.toDomain(response.getCatalog()));
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "createCatalog", ImmutableMap.of("catalogName", catalogName));
    }
  }

  @Override
  public Optional<Catalog> findCatalogByName(String catalogName) {
    try {
      FindCatalogRequest request =
          FindCatalogRequest.newBuilder().setCatalogName(catalogName).build();
      FindCatalogResponse response = stub.findCatalog(request);
      return response.hasCatalog()
          ? Optional.of(
              Objects.requireNonNull(CatalogMapper.INSTANCE.toDomain(response.getCatalog())))
          : Optional.empty();
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "findCatalogByName", ImmutableMap.of("catalogName", catalogName));
    }
  }

  @Override
  public List<Catalog> listAllCatalogs() {
    try {
      ListAllCatalogsResponse response =
          stub.listAllCatalogs(ListAllCatalogsRequest.getDefaultInstance());
      return response.getCatalogsList().stream()
          .map(CatalogMapper.INSTANCE::toDomain)
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(e, "listAllCatalogs");
    }
  }

  @Override
  public Optional<Catalog> findCatalogById(UUID catalogId) {
    try {
      FindCatalogByIdRequest request =
          FindCatalogByIdRequest.newBuilder().setCatalogId(catalogId.toString()).build();
      FindCatalogByIdResponse response = stub.findCatalogById(request);
      return response.hasCatalog()
          ? Optional.of(
              Objects.requireNonNull(CatalogMapper.INSTANCE.toDomain(response.getCatalog())))
          : Optional.empty();
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "findCatalogById", ImmutableMap.of("catalogId", catalogId.toString()));
    }
  }

  @Override
  public boolean deleteCatalogByName(String catalogName, boolean cascade) {
    try {
      DeleteCatalogRequest request =
          DeleteCatalogRequest.newBuilder().setCatalogName(catalogName).setCascade(cascade).build();
      DeleteCatalogResponse response = stub.deleteCatalog(request);
      return response.getDeleted();
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "deleteCatalogByName", ImmutableMap.of("catalogName", catalogName));
    }
  }

  @Override
  public boolean deleteCatalogById(UUID catalogId, boolean cascade) {
    try {
      DeleteCatalogByIdRequest request =
          DeleteCatalogByIdRequest.newBuilder()
              .setCatalogId(catalogId.toString())
              .setCascade(cascade)
              .build();
      DeleteCatalogByIdResponse response = stub.deleteCatalogById(request);
      return response.getDeleted();
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "deleteCatalogById", ImmutableMap.of("catalogId", catalogId.toString()));
    }
  }
}
