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

/**
 * The delete methods call {@code deleteById} without checking first whether the row exists, so
 * deleting a missing row must be a no-op on the backend. The server always runs its transactions at
 * the SERIALIZABLE isolation level, so these cases run under SERIALIZABLE.
 */
class DeleteMissingIntegrationTest extends AbstractScalarDbIntegrationTest {

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
