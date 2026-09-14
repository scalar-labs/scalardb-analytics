/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.jdbc.auth.internal;

import com.scalar.db.sql.springdata.ScalarDbRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface InternalCredentialJdbcRepository
    extends ScalarDbRepository<InternalCredentialEntity, String> {
  Optional<InternalCredentialEntity> findByUsername(String username);

  void deleteByUsername(String username);
}
