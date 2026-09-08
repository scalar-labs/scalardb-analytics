/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.datasource;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest;
import com.scalar.db.analytics.grpc.generated.datasource.v1.DataSourceServiceGrpc;
import com.scalar.db.analytics.grpc.generated.datasource.v1.DeleteDataSourceByIdRequest;
import com.scalar.db.analytics.grpc.generated.datasource.v1.DeleteDataSourceByIdResponse;
import com.scalar.db.analytics.grpc.generated.datasource.v1.DeleteDataSourceRequest;
import com.scalar.db.analytics.grpc.generated.datasource.v1.DeleteDataSourceResponse;
import com.scalar.db.analytics.grpc.generated.datasource.v1.FindDataSourceByIdRequest;
import com.scalar.db.analytics.grpc.generated.datasource.v1.FindDataSourceByIdResponse;
import com.scalar.db.analytics.grpc.generated.datasource.v1.FindDataSourceRequest;
import com.scalar.db.analytics.grpc.generated.datasource.v1.FindDataSourceResponse;
import com.scalar.db.analytics.grpc.generated.datasource.v1.ListDataSourcesRequest;
import com.scalar.db.analytics.grpc.generated.datasource.v1.RegisterResponse;
import com.scalar.db.analytics.grpc.mapper.RequestMapper;
import com.scalar.db.analytics.grpc.mapper.datasource.DataSourceMapper;
import com.scalar.db.analytics.sdk.exception.GrpcExceptionMapper;
import io.grpc.Channel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link DataSourceClient}.
 *
 * <p>This class is for internal SDK use only and should not be instantiated directly by SDK users.
 */
public class BlockingGrpcDataSourceClient implements DataSourceClient {
  private final DataSourceServiceGrpc.DataSourceServiceBlockingStub stub;

  public BlockingGrpcDataSourceClient(Channel channel) {
    this.stub = DataSourceServiceGrpc.newBlockingStub(channel);
  }

  @VisibleForTesting
  BlockingGrpcDataSourceClient(DataSourceServiceGrpc.DataSourceServiceBlockingStub stub) {
    this.stub = stub;
  }

  @Override
  public Optional<DataSource> findDataSourceByName(String catalogName, String dataSourceName) {
    try {
      FindDataSourceRequest request =
          FindDataSourceRequest.newBuilder()
              .setCatalogName(catalogName)
              .setDataSourceName(dataSourceName)
              .build();
      FindDataSourceResponse response = stub.findDataSource(request);
      return response.hasDataSource()
          ? Optional.of(DataSourceMapper.INSTANCE.toDomain(response.getDataSource()))
          : Optional.empty();
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e,
          "findDataSourceByName",
          ImmutableMap.of("catalogName", catalogName, "dataSourceName", dataSourceName));
    }
  }

  @Override
  public List<DataSource> listDataSourcesByCatalog(String catalogName) {
    try {
      ListDataSourcesRequest request =
          ListDataSourcesRequest.newBuilder().setCatalogName(catalogName).build();
      return stub.listDataSources(request).getDataSourcesList().stream()
          .map(DataSourceMapper.INSTANCE::toDomain)
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "listDataSourcesByCatalog", ImmutableMap.of("catalogName", catalogName));
    }
  }

  @Override
  public DataSource register(RegisterDataSourceRequest request) {
    try {
      RegisterResponse response =
          stub.register(RequestMapper.INSTANCE.toProtoRegisterRequest(request));
      return DataSourceMapper.INSTANCE.toDomain(response.getDataSource());
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(e, "register");
    }
  }

  @Override
  public Optional<DataSource> findDataSourceById(UUID dataSourceId) {
    try {
      FindDataSourceByIdRequest request =
          FindDataSourceByIdRequest.newBuilder().setDataSourceId(dataSourceId.toString()).build();
      FindDataSourceByIdResponse response = stub.findDataSourceById(request);
      return response.hasDataSource()
          ? Optional.of(DataSourceMapper.INSTANCE.toDomain(response.getDataSource()))
          : Optional.empty();
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "findDataSourceById", ImmutableMap.of("dataSourceId", dataSourceId.toString()));
    }
  }

  @Override
  public boolean deleteDataSourceByName(
      String catalogName, String dataSourceName, boolean cascade) {
    try {
      DeleteDataSourceRequest request =
          DeleteDataSourceRequest.newBuilder()
              .setCatalogName(catalogName)
              .setDataSourceName(dataSourceName)
              .setCascade(cascade)
              .build();
      DeleteDataSourceResponse response = stub.deleteDataSource(request);
      return response.getDeleted();
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e,
          "deleteDataSourceByName",
          ImmutableMap.of("catalogName", catalogName, "dataSourceName", dataSourceName));
    }
  }

  @Override
  public boolean deleteDataSourceById(UUID dataSourceId, boolean cascade) {
    try {
      DeleteDataSourceByIdRequest request =
          DeleteDataSourceByIdRequest.newBuilder()
              .setDataSourceId(dataSourceId.toString())
              .setCascade(cascade)
              .build();
      DeleteDataSourceByIdResponse response = stub.deleteDataSourceById(request);
      return response.getDeleted();
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "deleteDataSourceById", ImmutableMap.of("dataSourceId", dataSourceId.toString()));
    }
  }
}
