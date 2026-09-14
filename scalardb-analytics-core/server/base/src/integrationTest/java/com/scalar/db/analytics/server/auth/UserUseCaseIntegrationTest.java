/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.api.auth.UserDetail;
import com.scalar.db.analytics.api.auth.UserInfo;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.server.support.UseCaseIntegrationTestBase;
import com.scalar.db.analytics.usecase.auth.UserUseCase;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserUseCaseIntegrationTest extends UseCaseIntegrationTestBase {

  @Autowired private UserUseCase userUseCase;

  private UUID adminId;
  private UUID testUserId;

  @BeforeEach
  void setUp() {
    adminId = adminUserId();
    testUserId =
        userUseCase
            .createUserWithBackendUser(adminId, "user-test-alice", "user-test-alice", "alice-pass")
            .getUserId();
  }

  @AfterEach
  void tearDown() {
    try {
      userUseCase.deleteUser(adminId, "user-test-alice", true);
    } catch (AnalyticsException ignored) {
    }
  }

  @Test
  void listUsers_shouldReturnAllUsers() {
    List<UserInfo> users = userUseCase.listUsers(adminId);

    assertThat(users).extracting(UserInfo::getUsername).contains(ADMIN_USERNAME, "user-test-alice");
  }

  @Test
  void describeUserById_shouldReturnUserDetail() {
    Optional<UserDetail> detail = userUseCase.describeUserById(adminId, testUserId);

    assertThat(detail).isPresent();
    assertThat(detail.get().getUsername()).isEqualTo("user-test-alice");
    assertThat(detail.get().getBackend()).isEqualTo(PasswordBackendType.INTERNAL);
  }

  @Test
  void describeUserByName_shouldReturnUserDetail() {
    Optional<UserDetail> detail = userUseCase.describeUserByName(adminId, "user-test-alice");

    assertThat(detail).isPresent();
    assertThat(detail.get().getUserId()).isEqualTo(testUserId.toString());
  }

  @Test
  void describeNonExistentUser_shouldReturnEmpty() {
    Optional<UserDetail> detail = userUseCase.describeUserById(adminId, UUID.randomUUID());

    assertThat(detail).isEmpty();
  }
}
