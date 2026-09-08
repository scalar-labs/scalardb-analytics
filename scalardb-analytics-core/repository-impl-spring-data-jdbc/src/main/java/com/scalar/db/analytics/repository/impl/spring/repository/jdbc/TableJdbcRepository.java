/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.jdbc;

import com.scalar.db.sql.springdata.ScalarDbRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TableJdbcRepository extends ScalarDbRepository<TableEntity, UUID> {

  @Query(
      "SELECT table_id, namespace_id, name, created_at, updated_at "
          + "FROM registry_tables "
          + "WHERE namespace_id = :namespaceId")
  List<TableEntity> findByNamespaceId(@Param("namespaceId") UUID namespaceId);

  @Query(
      "SELECT table_id, namespace_id, name, created_at, updated_at "
          + "FROM registry_tables "
          + "WHERE namespace_id = :namespaceId AND name = :name")
  Optional<TableEntity> findByNamespaceIdAndName(
      @Param("namespaceId") UUID namespaceId, @Param("name") String name);
}
