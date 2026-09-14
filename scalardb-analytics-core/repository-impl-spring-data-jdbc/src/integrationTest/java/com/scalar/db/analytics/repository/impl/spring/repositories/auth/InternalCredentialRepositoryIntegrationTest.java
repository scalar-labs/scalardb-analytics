/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repositories.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.auth.internal.InternalCredential;
import com.scalar.db.analytics.repository.impl.spring.repository.auth.internal.InternalCredentialRepositoryImpl;
import com.scalar.db.analytics.repository.impl.spring.support.AbstractScalarDbIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InternalCredentialRepositoryIntegrationTest extends AbstractScalarDbIntegrationTest {

  @Autowired private InternalCredentialRepositoryImpl internalCredentialRepository;

  @Test
  void createShouldPersistCredential() {
    InternalCredential credential = new InternalCredential("testuser", "$2a$10$hashedpassword");

    internalCredentialRepository.create(ctx, credential);

    var found = internalCredentialRepository.findByUsername(ctx, "testuser");
    assertThat(found).isPresent();
    assertThat(found.get().username()).isEqualTo("testuser");
    assertThat(found.get().passwordHash()).isEqualTo("$2a$10$hashedpassword");
  }

  @Test
  void createDuplicateUsernameShouldThrow() {
    internalCredentialRepository.create(ctx, new InternalCredential("dupuser", "$2a$10$hash1"));

    assertThatThrownBy(
            () ->
                internalCredentialRepository.create(
                    ctx, new InternalCredential("dupuser", "$2a$10$hash2")))
        .isInstanceOf(AnalyticsException.class)
        .extracting(ex -> ((AnalyticsException) ex).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.USER_ALREADY_EXISTS);
  }

  @Test
  void findByUsernameShouldReturnEmptyWhenNotFound() {
    assertThat(internalCredentialRepository.findByUsername(ctx, "nonexistent")).isEmpty();
  }

  @Test
  void deleteByUsernameShouldRemoveCredential() {
    internalCredentialRepository.create(ctx, new InternalCredential("deleteuser", "$2a$10$hash"));

    internalCredentialRepository.deleteByUsername(ctx, "deleteuser");

    assertThat(internalCredentialRepository.findByUsername(ctx, "deleteuser")).isEmpty();
  }

  @Test
  void deleteByUsernameShouldNotThrowWhenNotFound() {
    assertThatCode(() -> internalCredentialRepository.deleteByUsername(ctx, "nonexistent"))
        .doesNotThrowAnyException();
  }
}
