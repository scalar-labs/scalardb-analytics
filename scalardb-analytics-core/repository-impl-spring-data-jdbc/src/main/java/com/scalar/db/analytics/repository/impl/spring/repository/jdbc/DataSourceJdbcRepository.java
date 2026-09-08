/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.jdbc;

import com.scalar.db.sql.springdata.ScalarDbRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DataSourceJdbcRepository extends ScalarDbRepository<DataSourceEntity, UUID> {

  @Query(
      "SELECT data_source_id, catalog_id, name, provider_type, provider_payload_json, "
          + "created_at, updated_at "
          + "FROM registry_data_sources "
          + "WHERE catalog_id = :catalogId AND name = :name")
  Optional<DataSourceEntity> findByCatalogIdAndName(
      @Param("catalogId") UUID catalogId, @Param("name") String name);
}
