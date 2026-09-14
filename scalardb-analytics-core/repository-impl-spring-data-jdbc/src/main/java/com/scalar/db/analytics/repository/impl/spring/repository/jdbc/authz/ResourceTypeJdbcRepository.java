/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.jdbc.authz;

import com.scalar.db.sql.springdata.ScalarDbRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceTypeJdbcRepository extends ScalarDbRepository<ResourceTypeEntity, UUID> {
  Optional<ResourceTypeEntity> findByName(String name);
}
