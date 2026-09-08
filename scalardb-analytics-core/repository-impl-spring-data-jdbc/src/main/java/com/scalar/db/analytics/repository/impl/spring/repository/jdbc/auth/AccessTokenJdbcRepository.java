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
public interface AccessTokenJdbcRepository extends ScalarDbRepository<AccessTokenEntity, UUID> {

  Optional<AccessTokenEntity> findByUserIdAndToken(UUID userId, String token);
}
