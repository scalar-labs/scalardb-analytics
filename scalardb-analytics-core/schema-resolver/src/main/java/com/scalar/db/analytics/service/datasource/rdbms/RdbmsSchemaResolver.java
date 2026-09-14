/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import com.scalar.db.analytics.api.model.Column;
import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.TableDetail;
import com.scalar.db.analytics.api.model.TableInfo;
import com.scalar.db.analytics.service.datasource.ResolvedSchema;
import com.scalar.db.analytics.service.datasource.ResolvedSchemaEntry;
import com.scalar.db.analytics.service.datasource.SchemaResolver;
import com.scalar.db.analytics.service.datasource.SchemaResolverException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * This class provides the common logic for schema resolvers for RDBMS data sources. The subclasses
 * should implement the abstract methods to resolve the namespaces.
 *
 * <p>It is user's responsibility to close the connection after using the schema resolver by calling
 * the {@code close()} method.
 */
@Slf4j
public abstract class RdbmsSchemaResolver implements SchemaResolver, AutoCloseable {
  private final UUID dataSourceId;
  private final String connectionUrl;
  private final Properties connectionProperties;
  private final RdbmsSchemaResolverOption option;
  private final RdbmsSchemaResolverStrategy strategy;

  // conn and metaData are populated by openConnection(), not the constructor; the resolver
  // contract requires openConnection() to be called before any schema resolution.
  @SuppressWarnings("NullAway.Init")
  private Connection conn;

  @SuppressWarnings("NullAway.Init")
  private JdbcMetaData metaData;

  /**
   * Constructs a schema resolver for an RDBMS data source.
   *
   * @param dataSourceId the ID of the data source
   * @param connectionUrl JDBC connection URL
   * @param connectionProperties JDBC connection properties
   * @param option the option for the schema resolver
   * @param strategy the strategy for the schema resolver
   */
  RdbmsSchemaResolver(
      UUID dataSourceId,
      String connectionUrl,
      Properties connectionProperties,
      RdbmsSchemaResolverOption option,
      RdbmsSchemaResolverStrategy strategy) {
    this.dataSourceId = dataSourceId;
    this.connectionUrl = connectionUrl;
    this.connectionProperties = connectionProperties;
    this.option = option;
    this.strategy = strategy;
  }

  @Override
  public ResolvedSchema resolveSchema() throws SchemaResolverException {
    List<ResolvedSchemaEntry> result =
        strategy.resolveNamespaces(metaData).stream()
            .filter(
                namespaceInfo -> {
                  return option.getNamespacesToResolve().isEmpty()
                      || option.getNamespacesToResolve().contains(namespaceInfo.toNamespaceNames());
                })
            .map(
                namespaceInfo -> {
                  Namespace namespace =
                      Namespace.create(dataSourceId, namespaceInfo.toNamespaceNames());

                  List<TableDetail> tableDetails =
                      resolveTables(namespaceInfo).stream()
                          .map(tableInfo -> getTableDetail(namespace.getId(), tableInfo))
                          .collect(Collectors.toList());
                  return new ResolvedSchemaEntry(namespace, tableDetails);
                })
            .collect(Collectors.toList());
    return new ResolvedSchema(result);
  }

  private TableDetail getTableDetail(UUID namespaceId, JdbcTableInfo jdbcTableInfo)
      throws SchemaResolverException {
    TableInfo tableInfo = TableInfo.create(namespaceId, jdbcTableInfo.getName());
    List<Column> columns = new ArrayList<>();
    for (int j = 0; j < jdbcTableInfo.getColumns().size(); j++) {
      Optional<Column> column =
          convertColumn(jdbcTableInfo.getColumns().get(j), tableInfo.getId(), j);
      column.ifPresent(columns::add);
    }
    return new TableDetail(tableInfo, columns);
  }

  private List<JdbcTableInfo> resolveTables(JdbcNamespaceInfo info) throws SchemaResolverException {
    try (ResultSet tables = metaData.getTables(info)) {
      return JdbcUtil.iterateOverResultSet(
          tables,
          rs -> {
            String tableName = metaData.getTablesTableName(rs);
            if (!option.getTablesToResolve().isEmpty()
                && !option.getTablesToResolve().contains(tableName)) {
              return null;
            }
            List<JdbcColumnInfo> columns = getColumnsForTable(info, tableName);
            return new JdbcTableInfo(tableName, columns);
          });
    } catch (SQLException e) {
      throw new JdbcException(e);
    }
  }

  private List<JdbcColumnInfo> getColumnsForTable(JdbcNamespaceInfo info, String tableName)
      throws SchemaResolverException {
    try (ResultSet columns = metaData.getColumns(info, tableName)) {
      return JdbcUtil.iterateOverResultSet(
          columns,
          rs -> {
            String columnName = metaData.getColumnsColumnName(rs);
            JDBCType jdbcType = JdbcUtil.getJdbcType(metaData.getColumnsDataType(rs));
            String columnTypeName = metaData.getColumnsTypeName(rs);
            return new JdbcColumnInfo(
                info,
                tableName,
                columnName,
                jdbcType,
                columnTypeName,
                metaData.getColumnsColumnSize(rs),
                metaData.getColumnsDecimalDigits(rs),
                metaData.getColumnsNullable(rs));
          });
    } catch (SQLException e) {
      throw new JdbcException(e);
    }
  }

  private Optional<Column> convertColumn(JdbcColumnInfo column, UUID tableId, int ordinalPosition)
      throws SchemaResolverException {
    DataType dataType;
    try {
      dataType = strategy.jdbcTypeToDataType(column);
    } catch (UnsupportedJdbcTypeException e) {
      if (option.getFailOnUnsupportedDataType()) {
        throw e;
      } else {
        logger.warn("The column '{}' is ignored. {}", column.getName(), e.getMessage());
        return Optional.empty();
      }
    }
    return Optional.of(
        Column.create(tableId, column.getName(), dataType, ordinalPosition, column.isNullable()));
  }

  protected void openConnection() throws SchemaResolverException {
    try {
      this.conn = DriverManager.getConnection(connectionUrl, connectionProperties);
      this.metaData = new JdbcMetaData(conn.getMetaData());
    } catch (SQLException e) {
      throw new JdbcException(e);
    }
  }

  @Override
  @SuppressWarnings("ConstantValue")
  public void close() throws SchemaResolverException {
    if (conn != null) {
      try {
        conn.close();
      } catch (SQLException e) {
        throw new JdbcException(e);
      }
    }
  }
}
