/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.jdbc;

import com.scalar.db.analytics.repository.impl.spring.converter.StringList;
import com.scalar.db.sql.springdata.ScalarDbRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NamespaceJdbcRepository extends ScalarDbRepository<NamespaceEntity, UUID> {

  @Query(
      "SELECT namespace_id, data_source_id, names, created_at, updated_at "
          + "FROM registry_namespaces "
          + "WHERE data_source_id = :dataSourceId AND names = :names")
  Optional<NamespaceEntity> findByDataSourceIdAndNames(
      @Param("dataSourceId") UUID dataSourceId, @Param("names") StringList names);
}
