/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repositories.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.analytics.api.auth.AuthUser;
import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.auth.PasswordIdentity;
import com.scalar.db.analytics.repository.impl.spring.repository.auth.AuthUserRepositoryImpl;
import com.scalar.db.analytics.repository.impl.spring.repository.auth.PasswordIdentityRepositoryImpl;
import com.scalar.db.analytics.repository.impl.spring.support.AbstractScalarDbIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PasswordIdentityRepositoryIntegrationTest extends AbstractScalarDbIntegrationTest {

  @Autowired private AuthUserRepositoryImpl authUserRepository;
  @Autowired private PasswordIdentityRepositoryImpl passwordIdentityRepository;

  private AuthUser testUser;

  @BeforeEach
  void setUpUser() {
    testUser = AuthUser.create("testuser");
    authUserRepository.create(ctx, testUser);
  }

  @Test
  void createShouldPersistIdentity() {
    PasswordIdentity identity =
        PasswordIdentity.create(
            testUser.getUserId(), PasswordBackendType.INTERNAL, "user@example.com");

    passwordIdentityRepository.create(ctx, identity);

    var found = passwordIdentityRepository.findByUserId(ctx, testUser.getUserId());
    assertThat(found).hasSize(1);
    assertThat(found.get(0).userId()).isEqualTo(testUser.getUserId());
    assertThat(found.get(0).backend()).isEqualTo(PasswordBackendType.INTERNAL);
    assertThat(found.get(0).backendUserId()).isEqualTo("user@example.com");
  }

  @Test
  void createDuplicateBackendForSameUserShouldThrowEntityAlreadyExists() {
    PasswordIdentity identity =
        PasswordIdentity.create(
            testUser.getUserId(), PasswordBackendType.INTERNAL, "user@example.com");
    passwordIdentityRepository.create(ctx, identity);

    PasswordIdentity duplicate =
        PasswordIdentity.create(
            testUser.getUserId(), PasswordBackendType.INTERNAL, "other@example.com");

    assertThatThrownBy(() -> passwordIdentityRepository.create(ctx, duplicate))
        .isInstanceOf(AnalyticsException.class)
        .extracting(ex -> ((AnalyticsException) ex).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.USER_ALREADY_EXISTS);
  }

  @Test
  void deleteByUserIdShouldRemoveAllIdentitiesForUser() {
    PasswordIdentity identity1 =
        PasswordIdentity.create(
            testUser.getUserId(), PasswordBackendType.INTERNAL, "user1@example.com");
    PasswordIdentity identity2 =
        PasswordIdentity.create(
            testUser.getUserId(), PasswordBackendType.SCALARDB_CLUSTER, "user1-cluster");
    passwordIdentityRepository.create(ctx, identity1);
    passwordIdentityRepository.create(ctx, identity2);

    passwordIdentityRepository.deleteByUserId(ctx, testUser.getUserId());

    assertThat(passwordIdentityRepository.findByUserId(ctx, testUser.getUserId())).isEmpty();
  }

  @Test
  void deleteByUserIdShouldNotThrowWhenNoIdentitiesExist() {
    assertThatCode(() -> passwordIdentityRepository.deleteByUserId(ctx, UUID.randomUUID()))
        .doesNotThrowAnyException();
  }

  @Test
  void findByUserIdShouldReturnAllIdentitiesForUser() {
    PasswordIdentity identity1 =
        PasswordIdentity.create(
            testUser.getUserId(), PasswordBackendType.INTERNAL, "user@example.com");
    PasswordIdentity identity2 =
        PasswordIdentity.create(
            testUser.getUserId(), PasswordBackendType.SCALARDB_CLUSTER, "cluster-user");
    passwordIdentityRepository.create(ctx, identity1);
    passwordIdentityRepository.create(ctx, identity2);

    var identities = passwordIdentityRepository.findByUserId(ctx, testUser.getUserId());

    assertThat(identities).hasSize(2);
    assertThat(identities)
        .extracting(PasswordIdentity::backend)
        .containsExactlyInAnyOrder(
            PasswordBackendType.INTERNAL, PasswordBackendType.SCALARDB_CLUSTER);
  }

  @Test
  void findByUserIdShouldReturnEmptyListWhenNoIdentitiesExist() {
    var identities = passwordIdentityRepository.findByUserId(ctx, UUID.randomUUID());

    assertThat(identities).isEmpty();
  }

  @Test
  void findByBackendAndBackendUserIdShouldReturnIdentityWhenExists() {
    PasswordIdentity identity =
        PasswordIdentity.create(
            testUser.getUserId(), PasswordBackendType.INTERNAL, "user@example.com");
    passwordIdentityRepository.create(ctx, identity);

    var found =
        passwordIdentityRepository.findByBackendAndBackendUserId(
            ctx, PasswordBackendType.INTERNAL, "user@example.com");

    assertThat(found).isPresent();
    assertThat(found.get().userId()).isEqualTo(testUser.getUserId());
    assertThat(found.get().backend()).isEqualTo(PasswordBackendType.INTERNAL);
    assertThat(found.get().backendUserId()).isEqualTo("user@example.com");
  }

  @Test
  void findByBackendAndBackendUserIdShouldReturnEmptyWhenNotExists() {
    var found =
        passwordIdentityRepository.findByBackendAndBackendUserId(
            ctx, PasswordBackendType.INTERNAL, "nonexistent@example.com");

    assertThat(found).isEmpty();
  }

  @Test
  void findByBackendAndBackendUserIdShouldDistinguishBackendsForSameBackendUserId() {
    // Same backendUserId string under two different backends. The natural-key lookup must
    // return the row matching the requested backend, not collide across backends.
    AuthUser otherUser = AuthUser.create("other");
    authUserRepository.create(ctx, otherUser);
    passwordIdentityRepository.create(
        ctx, PasswordIdentity.create(testUser.getUserId(), PasswordBackendType.INTERNAL, "alice"));
    passwordIdentityRepository.create(
        ctx,
        PasswordIdentity.create(
            otherUser.getUserId(), PasswordBackendType.SCALARDB_CLUSTER, "alice"));

    var internal =
        passwordIdentityRepository.findByBackendAndBackendUserId(
            ctx, PasswordBackendType.INTERNAL, "alice");
    var cluster =
        passwordIdentityRepository.findByBackendAndBackendUserId(
            ctx, PasswordBackendType.SCALARDB_CLUSTER, "alice");

    assertThat(internal).isPresent();
    assertThat(internal.get().userId()).isEqualTo(testUser.getUserId());
    assertThat(internal.get().backend()).isEqualTo(PasswordBackendType.INTERNAL);

    assertThat(cluster).isPresent();
    assertThat(cluster.get().userId()).isEqualTo(otherUser.getUserId());
    assertThat(cluster.get().backend()).isEqualTo(PasswordBackendType.SCALARDB_CLUSTER);
  }

  @Test
  void deleteByUserIdAndBackendShouldRemoveOnlyMatchingIdentity() {
    passwordIdentityRepository.create(
        ctx,
        PasswordIdentity.create(
            testUser.getUserId(), PasswordBackendType.INTERNAL, "alice@example.com"));
    passwordIdentityRepository.create(
        ctx,
        PasswordIdentity.create(
            testUser.getUserId(), PasswordBackendType.SCALARDB_CLUSTER, "alice-cluster"));

    passwordIdentityRepository.deleteByUserIdAndBackend(
        ctx, testUser.getUserId(), PasswordBackendType.INTERNAL);

    var remaining = passwordIdentityRepository.findByUserId(ctx, testUser.getUserId());
    assertThat(remaining).hasSize(1);
    assertThat(remaining.get(0).backend()).isEqualTo(PasswordBackendType.SCALARDB_CLUSTER);
    assertThat(remaining.get(0).backendUserId()).isEqualTo("alice-cluster");
  }

  @Test
  void deleteByUserIdAndBackendShouldNotThrowWhenNoMatch() {
    assertThatCode(
            () ->
                passwordIdentityRepository.deleteByUserIdAndBackend(
                    ctx, UUID.randomUUID(), PasswordBackendType.INTERNAL))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldRoundTripIdentityWithScalarDbClusterBackend() {
    PasswordIdentity identity =
        PasswordIdentity.create(
            testUser.getUserId(), PasswordBackendType.SCALARDB_CLUSTER, "cluster-user");

    passwordIdentityRepository.create(ctx, identity);

    var found = passwordIdentityRepository.findByUserId(ctx, testUser.getUserId());
    assertThat(found).hasSize(1);
    assertThat(found.get(0).backend()).isEqualTo(PasswordBackendType.SCALARDB_CLUSTER);
    assertThat(found.get(0).backendUserId()).isEqualTo("cluster-user");
  }

  @Test
  void shouldRoundTripIdentityWithSpecialCharactersInBackendUserId() {
    PasswordIdentity identity =
        PasswordIdentity.create(
            testUser.getUserId(), PasswordBackendType.INTERNAL, "user+tag@example.com");

    passwordIdentityRepository.create(ctx, identity);

    var found = passwordIdentityRepository.findByUserId(ctx, testUser.getUserId());
    assertThat(found).hasSize(1);
    assertThat(found.get(0).backendUserId()).isEqualTo("user+tag@example.com");
  }
}
