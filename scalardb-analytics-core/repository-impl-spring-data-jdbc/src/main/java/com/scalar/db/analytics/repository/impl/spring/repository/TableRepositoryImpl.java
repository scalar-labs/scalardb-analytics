/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository;

import static com.scalar.db.analytics.repository.impl.spring.constants.EntityTypes.TABLE;

import com.scalar.db.analytics.api.codec.datatype.DataTypeCodec;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.Table;
import com.scalar.db.analytics.api.model.TableDetail;
import com.scalar.db.analytics.api.model.TableInfo;
import com.scalar.db.analytics.repository.TableRepository;
import com.scalar.db.analytics.repository.impl.spring.exception.SpringDataJdbcErrorMapper;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.ColumnEntity;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.ColumnJdbcRepository;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.TableEntity;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.TableJdbcRepository;
import com.scalar.db.analytics.repository.impl.spring.transaction.SpringDataJdbcTransactionContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class TableRepositoryImpl implements TableRepository<SpringDataJdbcTransactionContext> {

  private final TableJdbcRepository springDataRepository;
  private final ColumnJdbcRepository columnRepository;
  private final SpringDataJdbcErrorMapper errorMapper;
  private final DataTypeCodec dataTypeCodec;

  public TableRepositoryImpl(
      TableJdbcRepository springDataRepository,
      ColumnJdbcRepository columnRepository,
      SpringDataJdbcErrorMapper errorMapper,
      DataTypeCodec dataTypeCodec) {
    this.springDataRepository = springDataRepository;
    this.columnRepository = columnRepository;
    this.errorMapper = errorMapper;
    this.dataTypeCodec = dataTypeCodec;
  }

  @Override
  public Optional<Table> findById(SpringDataJdbcTransactionContext ctx, UUID id) {
    try {
      return springDataRepository.findById(id).map(this::toModel);
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, TABLE, id.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, TABLE, id.toString());
    }
  }

  @Override
  public List<Table> listByNamespaceId(SpringDataJdbcTransactionContext ctx, UUID namespaceId) {
    try {
      return springDataRepository.findByNamespaceId(namespaceId).stream()
          .map(this::toModel)
          .collect(Collectors.toList());
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, TABLE, namespaceId.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, TABLE, namespaceId.toString());
    }
  }

  @Override
  public Optional<Table> findByNamespaceIdAndName(
      SpringDataJdbcTransactionContext ctx, UUID namespaceId, String name) {
    try {
      return springDataRepository.findByNamespaceIdAndName(namespaceId, name).map(this::toModel);
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, TABLE, namespaceId + "/" + name);
    } catch (Exception e) {
      throw errorMapper.mapException(e, TABLE, namespaceId + "/" + name);
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void create(SpringDataJdbcTransactionContext ctx, TableDetail tableDetail) {
    try {
      TableInfo info = tableDetail.getInfo();
      // ScalarDB SQL does not enforce UNIQUE constraints; guard natural-key collisions here.
      if (springDataRepository
          .findByNamespaceIdAndName(info.getNamespaceId(), info.getName())
          .isPresent()) {
        throw new AnalyticsException(
            AnalyticsErrorCode.TABLE_ALREADY_EXISTS, Map.of("table_name", info.getName()));
      }
      TableEntity entity = new TableEntity(info.getId(), info.getNamespaceId(), info.getName());
      springDataRepository.insert(entity);

      // Save columns - save them one by one to ensure they're treated as new entities
      for (com.scalar.db.analytics.api.model.Column column : tableDetail.getColumns()) {
        ColumnEntity columnEntity =
            new ColumnEntity(
                column.getId(),
                column.getTableId(),
                column.getName(),
                dataTypeCodec.serialize(column.getType()),
                column.getOrdinalPosition(),
                column.isNullable());
        columnRepository.insert(columnEntity);
      }
    } catch (AnalyticsException e) {
      // Preserve explicit EntityAlreadyExists signal for callers.
      throw e;
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, TABLE, null);
    } catch (Exception e) {
      throw errorMapper.mapException(e, TABLE, null);
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void deleteById(SpringDataJdbcTransactionContext ctx, UUID id) {
    try {
      // Check if exists before deleting to avoid database-specific errors
      if (springDataRepository.existsById(id)) {
        // First delete all columns associated with this table
        columnRepository.deleteByTableId(id);
        // Then delete the table itself
        springDataRepository.deleteById(id);
      }
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, TABLE, id.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, TABLE, id.toString());
    }
  }

  private Table toModel(TableEntity entity) {
    TableInfo info = new TableInfo(entity.getTableId(), entity.getNamespaceId(), entity.getName());
    return new Table(info);
  }
}
