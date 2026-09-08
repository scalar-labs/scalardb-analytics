/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.datasource.scalardb.schema;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.scalar.db.analytics.api.model.Column;
import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.TableDetail;
import com.scalar.db.analytics.api.model.TableInfo;
import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider;
import com.scalar.db.analytics.lib.functional.Throwables;
import com.scalar.db.analytics.service.datasource.ResolvedSchema;
import com.scalar.db.analytics.service.datasource.ResolvedSchemaEntry;
import com.scalar.db.analytics.service.datasource.SchemaResolver;
import com.scalar.db.analytics.service.datasource.SchemaResolverException;
import com.scalar.db.api.Admin;
import com.scalar.db.api.DistributedTransactionAdmin;
import com.scalar.db.api.TableMetadata;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.service.TransactionFactory;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ScalarDbSchemaResolver implements SchemaResolver {
  private final UUID dataSourceId;
  private final Properties configProperties;

  private static final ImmutableSet<String> DATABASES_TO_IGNORE =
      ImmutableSet.of("coordinator", "scalardb");

  // This constructor is mainly for testing purposes
  ScalarDbSchemaResolver(UUID dataSourceId, Properties configProperties) {
    this.dataSourceId = dataSourceId;
    this.configProperties = configProperties;
  }

  public static ScalarDbSchemaResolver create(UUID dataSourceId, ScalarDbProvider provider)
      throws SchemaResolverException {
    return new ScalarDbSchemaResolver(dataSourceId, provider.toProperties());
  }

  @Override
  public ResolvedSchema resolveSchema() throws SchemaResolverException {
    try {
      TransactionFactory factory = TransactionFactory.create(configProperties);
      try (DistributedTransactionAdmin admin = factory.getTransactionAdmin()) {
        return resolveAllTables(admin);
      }
    } catch (ExecutionException e) {
      throw new SchemaResolverException("Failed to fetch schema information of ScalarDB", e);
    }
  }

  private ResolvedSchema resolveAllTables(Admin admin) throws ExecutionException {
    List<ResolvedSchemaEntry> entries =
        admin.getNamespaceNames().stream()
            .filter(namespace -> !DATABASES_TO_IGNORE.contains(namespace))
            .map(
                Throwables.sneakyThrowableFunction(
                    namespace -> resolveSchemaForNamespace(admin, namespace)))
            .collect(Collectors.toList());
    return new ResolvedSchema(entries);
  }

  private ResolvedSchemaEntry resolveSchemaForNamespace(Admin admin, String namespaceName)
      throws ExecutionException {
    Namespace namespace = Namespace.create(dataSourceId, Lists.newArrayList(namespaceName));
    List<TableDetail> tables =
        admin.getNamespaceTableNames(namespaceName).stream()
            .map(
                Throwables.sneakyThrowableFunction(
                    tableName -> resolveTableDetail(admin, namespace, namespaceName, tableName)))
            .collect(Collectors.toList());
    return new ResolvedSchemaEntry(namespace, tables);
  }

  private TableDetail resolveTableDetail(
      Admin admin, Namespace namespace, String namespaceName, String tableName)
      throws ExecutionException {
    TableInfo tableInfo = TableInfo.create(namespace.getId(), tableName);
    TableMetadata metadata =
        Objects.requireNonNull(admin.getTableMetadata(namespaceName, tableName));
    List<Column> columns = resolveColumns(tableInfo.getId(), metadata);
    return new TableDetail(tableInfo, columns);
  }

  private List<Column> resolveColumns(UUID tableId, TableMetadata metadata) {
    Set<String> partitionKeys = metadata.getPartitionKeyNames();
    Set<String> clusteringKeyNames = metadata.getClusteringKeyNames();

    int index = 0;
    ImmutableList.Builder<Column> columns = ImmutableList.builder();
    for (String columnName : metadata.getColumnNames()) {
      DataType type = TypeMapping.toModelType(metadata.getColumnDataType(columnName));
      boolean nullable =
          !partitionKeys.contains(columnName) && !clusteringKeyNames.contains(columnName);
      columns.add(Column.create(tableId, columnName, type, index, nullable));
      index++;
    }
    return columns.build();
  }
}
