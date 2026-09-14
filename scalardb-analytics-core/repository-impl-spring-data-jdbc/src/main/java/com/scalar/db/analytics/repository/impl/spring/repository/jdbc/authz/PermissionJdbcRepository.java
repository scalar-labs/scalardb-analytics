/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.jdbc.authz;

import com.scalar.db.sql.springdata.ScalarDbRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionJdbcRepository extends ScalarDbRepository<PermissionEntity, UUID> {
  Optional<PermissionEntity> findByName(String name);

  List<PermissionEntity> findByResourceTypeId(UUID resourceTypeId);
}
