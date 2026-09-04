/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repositories.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.scalar.db.analytics.api.auth.AuthUser;
import com.scalar.db.analytics.repository.impl.spring.repository.auth.AuthUserRepositoryImpl;
import com.scalar.db.analytics.repository.impl.spring.support.AbstractScalarDbIntegrationTest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuthUserRepositoryIntegrationTest extends AbstractScalarDbIntegrationTest {

  @Autowired private AuthUserRepositoryImpl authUserRepository;

  @Test
  void createShouldPersistUser() {
    AuthUser user = AuthUser.create("testuser");

    authUserRepository.create(ctx, user);

    Optional<AuthUser> found = authUserRepository.findById(ctx, user.getUserId());
    assertThat(found).isPresent();
    assertThat(found.get().getUserId()).isEqualTo(user.getUserId());
    assertThat(found.get().getUsername()).isEqualTo("testuser");
  }

  @Test
  void findByIdShouldReturnEmptyWhenMissing() {
    assertThat(authUserRepository.findById(ctx, UUID.randomUUID())).isEmpty();
  }

  @Test
  void deleteByIdShouldRemoveUser() {
    AuthUser user = AuthUser.create("testuser");
    authUserRepository.create(ctx, user);

    authUserRepository.deleteById(ctx, user.getUserId());

    assertThat(authUserRepository.findById(ctx, user.getUserId())).isEmpty();
  }

  @Test
  void deleteByIdShouldNotThrowWhenMissing() {
    assertThatCode(() -> authUserRepository.deleteById(ctx, UUID.randomUUID()))
        .doesNotThrowAnyException();
  }

  @Test
  void findByUsernameShouldReturnUserWhenExists() {
    AuthUser user = AuthUser.create("findme");
    authUserRepository.create(ctx, user);

    Optional<AuthUser> found = authUserRepository.findByUsername(ctx, "findme");
    assertThat(found).isPresent();
    assertThat(found.get().getUserId()).isEqualTo(user.getUserId());
    assertThat(found.get().getUsername()).isEqualTo("findme");
  }

  @Test
  void findByUsernameShouldReturnEmptyWhenNotExists() {
    assertThat(authUserRepository.findByUsername(ctx, "nonexistent")).isEmpty();
  }

  @Test
  void findAllShouldReturnAllPrincipalsViaCrossPartitionScan() {
    AuthUser alice = AuthUser.create("alice");
    AuthUser bob = AuthUser.create("bob");
    AuthUser carol = AuthUser.create("carol");
    authUserRepository.create(ctx, alice);
    authUserRepository.create(ctx, bob);
    authUserRepository.create(ctx, carol);

    java.util.List<AuthUser> all = authUserRepository.findAll(ctx);

    assertThat(all)
        .extracting(AuthUser::getUsername)
        .containsExactlyInAnyOrder("alice", "bob", "carol");
  }
}
