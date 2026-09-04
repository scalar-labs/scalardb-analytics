/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.jdbc.auth;

import com.scalar.db.sql.springdata.ScalarDbRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJdbcRepository extends ScalarDbRepository<UserEntity, UUID> {
  Optional<UserEntity> findByUsername(String username);
}
