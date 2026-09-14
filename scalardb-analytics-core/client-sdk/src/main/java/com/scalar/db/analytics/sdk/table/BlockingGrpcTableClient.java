/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.table;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTable;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTableDetail;
import com.scalar.db.analytics.grpc.generated.table.v1.DescribeTableByIdRequest;
import com.scalar.db.analytics.grpc.generated.table.v1.DescribeTableByIdResponse;
import com.scalar.db.analytics.grpc.generated.table.v1.DescribeTableRequest;
import com.scalar.db.analytics.grpc.generated.table.v1.DescribeTableResponse;
import com.scalar.db.analytics.grpc.generated.table.v1.ListTablesByNamespaceRequest;
import com.scalar.db.analytics.grpc.generated.table.v1.ListTablesRequest;
import com.scalar.db.analytics.grpc.generated.table.v1.TableServiceGrpc;
import com.scalar.db.analytics.grpc.mapper.table.TableMapper;
import com.scalar.db.analytics.sdk.exception.GrpcExceptionMapper;
import io.grpc.Channel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link TableClient}.
 *
 * <p>This class is for internal SDK use only and should not be instantiated directly by SDK users.
 */
public class BlockingGrpcTableClient implements TableClient {
  private final TableServiceGrpc.TableServiceBlockingStub stub;

  public BlockingGrpcTableClient(Channel channel) {
    this.stub = TableServiceGrpc.newBlockingStub(channel);
  }

  @VisibleForTesting
  BlockingGrpcTableClient(TableServiceGrpc.TableServiceBlockingStub stub) {
    this.stub = stub;
  }

  @Override
  public List<DataSourceNamespaceTable> listTablesByCatalog(String catalogName) {
    try {
      ListTablesRequest request =
          ListTablesRequest.newBuilder().setCatalogName(catalogName).build();

      return stub.listTables(request).getTablesList().stream()
          .map(TableMapper.INSTANCE::toDomain)
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "listTablesByCatalog", ImmutableMap.of("catalogName", catalogName));
    }
  }

  @Override
  public List<DataSourceNamespaceTable> listTablesByNamespace(
      String catalogName, String dataSourceName, List<String> namespaceNames) {
    try {
      ListTablesByNamespaceRequest request =
          ListTablesByNamespaceRequest.newBuilder()
              .setCatalogName(catalogName)
              .setDataSourceName(dataSourceName)
              .addAllNamespaceNames(namespaceNames)
              .build();

      return stub.listTablesByNamespace(request).getTablesList().stream()
          .map(TableMapper.INSTANCE::toDomain)
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e,
          "listTablesByNamespace",
          ImmutableMap.of("catalogName", catalogName, "dataSourceName", dataSourceName));
    }
  }

  @Override
  public Optional<DataSourceNamespaceTableDetail> describeTableByName(
      String catalogName, String dataSourceName, List<String> namespaceNames, String tableName) {
    try {
      DescribeTableRequest request =
          DescribeTableRequest.newBuilder()
              .setCatalogName(catalogName)
              .setDataSourceName(dataSourceName)
              .addAllNamespaceNames(namespaceNames)
              .setTableName(tableName)
              .build();

      DescribeTableResponse response = stub.describeTable(request);
      return response.hasTable()
          ? Optional.of(TableMapper.INSTANCE.toDomain(response.getTable()))
          : Optional.empty();
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e,
          "describeTableByName",
          ImmutableMap.of(
              "catalogName",
              catalogName,
              "dataSourceName",
              dataSourceName,
              "tableName",
              tableName));
    }
  }

  @Override
  public Optional<DataSourceNamespaceTableDetail> describeTableById(UUID tableId) {
    try {
      DescribeTableByIdRequest request =
          DescribeTableByIdRequest.newBuilder().setTableId(tableId.toString()).build();

      DescribeTableByIdResponse response = stub.describeTableById(request);
      return response.hasTable()
          ? Optional.of(TableMapper.INSTANCE.toDomain(response.getTable()))
          : Optional.empty();
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "describeTableById", ImmutableMap.of("tableId", tableId.toString()));
    }
  }
}
