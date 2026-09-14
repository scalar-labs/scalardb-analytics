/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository;

import static com.scalar.db.analytics.repository.impl.spring.constants.EntityTypes.COLUMN;

import com.scalar.db.analytics.api.codec.datatype.DataTypeCodec;
import com.scalar.db.analytics.api.model.Column;
import com.scalar.db.analytics.repository.ColumnRepository;
import com.scalar.db.analytics.repository.impl.spring.exception.SpringDataJdbcErrorMapper;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.ColumnEntity;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.ColumnJdbcRepository;
import com.scalar.db.analytics.repository.impl.spring.transaction.SpringDataJdbcTransactionContext;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class ColumnRepositoryImpl implements ColumnRepository<SpringDataJdbcTransactionContext> {

  private final ColumnJdbcRepository springDataRepository;
  private final SpringDataJdbcErrorMapper errorMapper;
  private final DataTypeCodec dataTypeCodec;

  public ColumnRepositoryImpl(
      ColumnJdbcRepository springDataRepository,
      SpringDataJdbcErrorMapper errorMapper,
      DataTypeCodec dataTypeCodec) {
    this.springDataRepository = springDataRepository;
    this.errorMapper = errorMapper;
    this.dataTypeCodec = dataTypeCodec;
  }

  @Override
  public List<Column> listByTableId(SpringDataJdbcTransactionContext ctx, UUID tableId) {
    try {
      return springDataRepository.findByTableId(tableId).stream()
          .sorted(Comparator.comparingInt(ColumnEntity::getOrdinalPosition))
          .map(this::toModel)
          .collect(Collectors.toList());
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, COLUMN, tableId.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, COLUMN, tableId.toString());
    }
  }

  private Column toModel(ColumnEntity entity) {
    return new Column(
        entity.getColumnId(),
        entity.getTableId(),
        entity.getName(),
        dataTypeCodec.deserialize(entity.getType()),
        entity.getOrdinalPosition(),
        entity.isNullable());
  }
}
