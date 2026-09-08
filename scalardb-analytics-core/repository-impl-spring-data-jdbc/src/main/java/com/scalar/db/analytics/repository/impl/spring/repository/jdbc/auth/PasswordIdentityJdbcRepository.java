/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.jdbc.auth;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.sql.springdata.ScalarDbRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordIdentityJdbcRepository
    extends ScalarDbRepository<PasswordIdentityEntity, UUID> {
  Optional<PasswordIdentityEntity> findByBackendAndBackendUserId(
      PasswordBackendType backend, String backendUserId);

  List<PasswordIdentityEntity> findByUserId(UUID userId);

  List<PasswordIdentityEntity> findByBackend(PasswordBackendType backend);

  void deleteByUserId(UUID userId);

  void deleteByUserIdAndBackend(UUID userId, PasswordBackendType backend);
}
