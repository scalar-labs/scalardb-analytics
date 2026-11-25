/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.repository.impl.spring.repository.auth.AccessTokenRepositoryImpl;
import com.scalar.db.analytics.repository.impl.spring.repository.auth.AuthUserRepositoryImpl;
import com.scalar.db.analytics.repository.impl.spring.repository.authz.RoleRepositoryImpl;
import com.scalar.db.analytics.repository.impl.spring.support.AbstractScalarDbIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * The delete methods call {@code deleteById} without checking first whether the row exists, so
 * deleting a missing row must be a no-op on the backend. The per-repository integration tests cover
 * this under the default isolation level; subclasses run the same cases under the other isolation
 * levels Consensus Commit supports.
 *
 * <p>Each subclass connects with its own ScalarDB configuration, so the ScalarDB SQL JDBC driver
 * creates separate connection pools for it. With the default pool sizes each set of pools opens
 * about 30 PostgreSQL connections up front, and together with the pools of the other test contexts
 * that exhausted the connections of the shared test container, so these tests let the pools start
 * empty.
 */
@TestPropertySource(
    properties = {
      "scalar.db.analytics.server.db.parameters[scalar.db.jdbc.connection_pool.min_idle]=0",
      "scalar.db.analytics.server.db.parameters"
          + "[scalar.db.jdbc.table_metadata.connection_pool.min_idle]=0",
      "scalar.db.analytics.server.db.parameters[scalar.db.jdbc.admin.connection_pool.min_idle]=0"
    })
abstract class AbstractDeleteMissingIntegrationTest extends AbstractScalarDbIntegrationTest {

  @Autowired private AccessTokenRepositoryImpl accessTokenRepository;
  @Autowired private AuthUserRepositoryImpl authUserRepository;
  @Autowired private RoleRepositoryImpl roleRepository;

  @Test
  void catalogDeleteByIdShouldNotThrowWhenMissing() {
    assertThatCode(() -> catalogRepository.deleteById(ctx, UUID.randomUUID()))
        .doesNotThrowAnyException();
  }

  // Deleting a missing row is handled by Consensus Commit in the same way for every table, so one
  // table is enough to check that it leaves nothing behind that blocks a later insert of the id.
  @Test
  void catalogDeleteByIdOfMissingIdShouldNotBlockLaterCreateWithSameId() {
    UUID catalogId = UUID.randomUUID();
    catalogRepository.deleteById(ctx, catalogId);

    catalogRepository.create(ctx, Catalog.of(catalogId, "created-after-delete"));

    assertThat(catalogRepository.findById(ctx, catalogId)).isPresent();
  }

  @Test
  void dataSourceDeleteByIdShouldNotThrowWhenMissing() {
    assertThatCode(() -> dataSourceRepository.deleteById(ctx, UUID.randomUUID()))
        .doesNotThrowAnyException();
  }

  @Test
  void namespaceDeleteByIdShouldNotThrowWhenMissing() {
    assertThatCode(() -> namespaceRepository.deleteById(ctx, UUID.randomUUID()))
        .doesNotThrowAnyException();
  }

  @Test
  void tableDeleteByIdShouldNotThrowWhenMissing() {
    assertThatCode(() -> tableRepository.deleteById(ctx, UUID.randomUUID()))
        .doesNotThrowAnyException();
  }

  @Test
  void accessTokenDeleteByUserIdShouldNotThrowWhenNoTokensExist() {
    assertThatCode(() -> accessTokenRepository.deleteByUserId(ctx, UUID.randomUUID()))
        .doesNotThrowAnyException();
  }

  @Test
  void authUserDeleteByIdShouldNotThrowWhenMissing() {
    assertThatCode(() -> authUserRepository.deleteById(ctx, UUID.randomUUID()))
        .doesNotThrowAnyException();
  }

  @Test
  void roleDeleteByIdShouldNotThrowWhenMissing() {
    assertThatCode(() -> roleRepository.deleteById(ctx, UUID.randomUUID()))
        .doesNotThrowAnyException();
  }
}
