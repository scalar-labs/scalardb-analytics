/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.jdbc;

import com.scalar.db.sql.springdata.ScalarDbRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface ColumnJdbcRepository extends ScalarDbRepository<ColumnEntity, UUID> {
  List<ColumnEntity> findByTableId(UUID tableId);

  void deleteByTableId(UUID tableId);
}
