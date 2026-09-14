/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.table;

import com.scalar.db.analytics.api.model.Column;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTable;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTableDetail;
import com.scalar.db.analytics.api.model.Table;
import com.scalar.db.analytics.api.model.TableDetail;
import com.scalar.db.analytics.api.model.TableInfo;
import com.scalar.db.analytics.grpc.mapper.UuidMapper;
import com.scalar.db.analytics.grpc.mapper.annotation.IgnoreProtobufBuilderDefaults;
import com.scalar.db.analytics.grpc.mapper.config.MapStructConfig;
import com.scalar.db.analytics.grpc.mapper.datasource.DataSourceMapper;
import com.scalar.db.analytics.grpc.mapper.datatype.DataTypeJsonMapper;
import com.scalar.db.analytics.grpc.mapper.namespace.NamespaceMapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper for converting between domain table models and protobuf messages.
 *
 * <p>This mapper handles the conversion of table-related objects including:
 *
 * <ul>
 *   <li>TableInfo - Basic metadata about a table
 *   <li>Table - Simple table with info only
 *   <li>Column - Column definition with type and constraints
 *   <li>TableDetail - Full table with column details
 *   <li>DataSourceNamespaceTable - Table with its namespace and data source
 *   <li>DataSourceNamespaceTableDetail - Detailed table with namespace and data source
 * </ul>
 */
@Mapper(
    config = MapStructConfig.class,
    uses = {
      UuidMapper.class,
      DataTypeJsonMapper.class,
      DataSourceMapper.class,
      NamespaceMapper.class
    })
public interface TableMapper {

  TableMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(TableMapper.class);

  // TableInfo mappings
  @IgnoreProtobufBuilderDefaults
  @Mapping(target = "id", source = "id", qualifiedByName = "uuidToString")
  @Mapping(target = "namespaceId", source = "namespaceId", qualifiedByName = "uuidToString")
  @Mapping(target = "idBytes", ignore = true)
  @Mapping(target = "namespaceIdBytes", ignore = true)
  @Mapping(target = "nameBytes", ignore = true)
  com.scalar.db.analytics.grpc.generated.table.v1.TableInfo toProto(TableInfo tableInfo);

  @Mapping(target = "id", source = "id", qualifiedByName = "stringToUuid")
  @Mapping(target = "namespaceId", source = "namespaceId", qualifiedByName = "stringToUuid")
  TableInfo toDomain(com.scalar.db.analytics.grpc.generated.table.v1.TableInfo tableInfo);

  // Table mappings
  @IgnoreProtobufBuilderDefaults
  @Mapping(target = "mergeInfo", ignore = true)
  com.scalar.db.analytics.grpc.generated.table.v1.Table toProto(Table table);

  Table toDomain(com.scalar.db.analytics.grpc.generated.table.v1.Table table);

  // Column mappings
  @IgnoreProtobufBuilderDefaults
  @Mapping(target = "id", source = "id", qualifiedByName = "uuidToString")
  @Mapping(target = "tableId", source = "tableId", qualifiedByName = "uuidToString")
  @Mapping(target = "type", source = "type", qualifiedByName = "dataTypeToJson")
  @Mapping(target = "idBytes", ignore = true)
  @Mapping(target = "tableIdBytes", ignore = true)
  @Mapping(target = "nameBytes", ignore = true)
  @Mapping(target = "typeBytes", ignore = true)
  com.scalar.db.analytics.grpc.generated.table.v1.Column toProto(Column column);

  @Mapping(target = "id", source = "id", qualifiedByName = "stringToUuid")
  @Mapping(target = "tableId", source = "tableId", qualifiedByName = "stringToUuid")
  @Mapping(target = "type", source = "type", qualifiedByName = "jsonToDataType")
  Column toDomain(com.scalar.db.analytics.grpc.generated.table.v1.Column column);

  // TableDetail mappings
  @IgnoreProtobufBuilderDefaults
  @Mapping(target = "mergeInfo", ignore = true)
  @Mapping(target = "removeColumns", ignore = true)
  @Mapping(target = "columnsList", ignore = true)
  @Mapping(target = "columnsOrBuilderList", ignore = true)
  @Mapping(target = "columnsBuilderList", ignore = true)
  com.scalar.db.analytics.grpc.generated.table.v1.TableDetail toProto(TableDetail tableDetail);

  @AfterMapping
  default void setColumns(
      @MappingTarget com.scalar.db.analytics.grpc.generated.table.v1.TableDetail.Builder builder,
      TableDetail source) {
    if (source.getColumns() != null) {
      // Add null check for each column element
      for (Column column : source.getColumns()) {
        if (column != null) {
          builder.addColumns(toProto(column));
        }
      }
    }
  }

  @Mapping(target = "columns", source = "columnsList")
  TableDetail toDomain(com.scalar.db.analytics.grpc.generated.table.v1.TableDetail tableDetail);

  // DataSourceNamespaceTable mappings
  @IgnoreProtobufBuilderDefaults
  @Mapping(target = "mergeDataSource", ignore = true)
  @Mapping(target = "mergeNamespace", ignore = true)
  @Mapping(target = "mergeTable", ignore = true)
  com.scalar.db.analytics.grpc.generated.table.v1.DataSourceNamespaceTable toProto(
      DataSourceNamespaceTable dataSourceNamespaceTable);

  DataSourceNamespaceTable toDomain(
      com.scalar.db.analytics.grpc.generated.table.v1.DataSourceNamespaceTable
          dataSourceNamespaceTable);

  // DataSourceNamespaceTableDetail mappings
  @IgnoreProtobufBuilderDefaults
  @Mapping(target = "tableDetail", source = "table")
  @Mapping(target = "mergeDataSource", ignore = true)
  @Mapping(target = "mergeNamespace", ignore = true)
  @Mapping(target = "mergeTableDetail", ignore = true)
  com.scalar.db.analytics.grpc.generated.table.v1.DataSourceNamespaceTableDetail toProto(
      DataSourceNamespaceTableDetail dataSourceNamespaceTableDetail);

  @Mapping(target = "table", source = "tableDetail")
  DataSourceNamespaceTableDetail toDomain(
      com.scalar.db.analytics.grpc.generated.table.v1.DataSourceNamespaceTableDetail
          dataSourceNamespaceTableDetail);
}
