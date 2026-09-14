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
public interface RoleJdbcRepository extends ScalarDbRepository<RoleEntity, UUID> {
  Optional<RoleEntity> findByName(String name);

  List<RoleEntity> findByBuiltInTrue();

  List<RoleEntity> findByBuiltInFalse();
}
