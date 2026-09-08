/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repositories.authz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.authz.AccessControlEntry;
import com.scalar.db.analytics.domain.authz.GranteeType;
import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.repository.impl.spring.repository.authz.AccessControlEntryRepositoryImpl;
import com.scalar.db.analytics.repository.impl.spring.support.AbstractScalarDbIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AccessControlEntryRepositoryIntegrationTest extends AbstractScalarDbIntegrationTest {

  @Autowired private AccessControlEntryRepositoryImpl accessControlEntryRepository;

  @Test
  void createShouldPersistEntry() {
    UUID granteeId = UUID.randomUUID();
    UUID resourceId = UUID.randomUUID();
    AccessControlEntry entry =
        AccessControlEntry.create(GranteeType.USER, granteeId, resourceId, Permission.CATALOG_READ);

    accessControlEntryRepository.create(ctx, entry);

    var found = accessControlEntryRepository.findByGrantee(ctx, GranteeType.USER, granteeId);
    assertThat(found).hasSize(1);
    assertThat(found.get(0).granteeType()).isEqualTo(GranteeType.USER);
    assertThat(found.get(0).granteeId()).isEqualTo(granteeId);
    assertThat(found.get(0).resourceId()).isEqualTo(resourceId);
    assertThat(found.get(0).permission()).isEqualTo(Permission.CATALOG_READ);
  }

  @Test
  void createDuplicateGranteeSamePermissionSameResourceShouldThrowEntityAlreadyExists() {
    UUID granteeId = UUID.randomUUID();
    UUID resourceId = UUID.randomUUID();
    AccessControlEntry entry =
        AccessControlEntry.create(GranteeType.USER, granteeId, resourceId, Permission.TABLE_READ);
    accessControlEntryRepository.create(ctx, entry);

    AccessControlEntry duplicate =
        AccessControlEntry.create(GranteeType.USER, granteeId, resourceId, Permission.TABLE_READ);

    assertThatThrownBy(() -> accessControlEntryRepository.create(ctx, duplicate))
        .isInstanceOf(AnalyticsException.class)
        .extracting(ex -> ((AnalyticsException) ex).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.PERMISSION_ALREADY_GRANTED);
  }

  @Test
  void createSameGranteeSameResourceDifferentPermissionShouldSucceed() {
    UUID granteeId = UUID.randomUUID();
    UUID resourceId = UUID.randomUUID();
    accessControlEntryRepository.create(
        ctx,
        AccessControlEntry.create(
            GranteeType.USER, granteeId, resourceId, Permission.CATALOG_READ));

    assertThatCode(
            () ->
                accessControlEntryRepository.create(
                    ctx,
                    AccessControlEntry.create(
                        GranteeType.USER, granteeId, resourceId, Permission.CATALOG_ADMIN)))
        .doesNotThrowAnyException();
  }

  @Test
  void findByGranteeShouldReturnMatchingEntries() {
    UUID granteeId = UUID.randomUUID();
    UUID resource1 = UUID.randomUUID();
    UUID resource2 = UUID.randomUUID();
    accessControlEntryRepository.create(
        ctx,
        AccessControlEntry.create(GranteeType.USER, granteeId, resource1, Permission.CATALOG_READ));
    accessControlEntryRepository.create(
        ctx,
        AccessControlEntry.create(
            GranteeType.USER, granteeId, resource2, Permission.NAMESPACE_READ));

    var entries = accessControlEntryRepository.findByGrantee(ctx, GranteeType.USER, granteeId);

    assertThat(entries).hasSize(2);
  }

  @Test
  void deleteByGranteeShouldRemoveAllEntriesForGrantee() {
    UUID granteeId = UUID.randomUUID();
    accessControlEntryRepository.create(
        ctx,
        AccessControlEntry.create(
            GranteeType.USER, granteeId, UUID.randomUUID(), Permission.CATALOG_READ));
    accessControlEntryRepository.create(
        ctx,
        AccessControlEntry.create(
            GranteeType.USER, granteeId, UUID.randomUUID(), Permission.TABLE_READ));

    accessControlEntryRepository.deleteByGrantee(ctx, GranteeType.USER, granteeId);

    assertThat(accessControlEntryRepository.findByGrantee(ctx, GranteeType.USER, granteeId))
        .isEmpty();
  }

  @Test
  void deleteByGranteeShouldNotThrowWhenNoEntries() {
    assertThatCode(
            () ->
                accessControlEntryRepository.deleteByGrantee(
                    ctx, GranteeType.USER, UUID.randomUUID()))
        .doesNotThrowAnyException();
  }

  @Test
  void deleteByResourceIdShouldRemoveAllEntriesForResource() {
    UUID grantee1 = UUID.randomUUID();
    UUID grantee2 = UUID.randomUUID();
    UUID resourceId = UUID.randomUUID();
    UUID otherResourceId = UUID.randomUUID();

    // Create ACEs for the target resource from different grantees
    accessControlEntryRepository.create(
        ctx,
        AccessControlEntry.create(GranteeType.USER, grantee1, resourceId, Permission.CATALOG_READ));
    accessControlEntryRepository.create(
        ctx,
        AccessControlEntry.create(
            GranteeType.ROLE, grantee2, resourceId, Permission.CATALOG_ADMIN));

    // Create ACE for a different resource (should not be affected)
    accessControlEntryRepository.create(
        ctx,
        AccessControlEntry.create(
            GranteeType.USER, grantee1, otherResourceId, Permission.TABLE_READ));

    accessControlEntryRepository.deleteByResourceId(ctx, resourceId);

    // Verify target resource ACEs are deleted
    assertThat(accessControlEntryRepository.findByGrantee(ctx, GranteeType.USER, grantee1))
        .hasSize(1)
        .allSatisfy(entry -> assertThat(entry.resourceId()).isEqualTo(otherResourceId));
    assertThat(accessControlEntryRepository.findByGrantee(ctx, GranteeType.ROLE, grantee2))
        .isEmpty();
  }

  @Test
  void deleteByResourceIdShouldNotThrowWhenNoEntries() {
    assertThatCode(() -> accessControlEntryRepository.deleteByResourceId(ctx, UUID.randomUUID()))
        .doesNotThrowAnyException();
  }

  @Test
  void deleteShouldRemoveSpecificEntry() {
    UUID granteeId = UUID.randomUUID();
    UUID resource1 = UUID.randomUUID();
    UUID resource2 = UUID.randomUUID();
    accessControlEntryRepository.create(
        ctx,
        AccessControlEntry.create(GranteeType.USER, granteeId, resource1, Permission.CATALOG_READ));
    accessControlEntryRepository.create(
        ctx,
        AccessControlEntry.create(
            GranteeType.USER, granteeId, resource2, Permission.NAMESPACE_READ));

    accessControlEntryRepository.delete(
        ctx, GranteeType.USER, granteeId, resource1, Permission.CATALOG_READ);

    var remaining = accessControlEntryRepository.findByGrantee(ctx, GranteeType.USER, granteeId);
    assertThat(remaining).hasSize(1);
    assertThat(remaining.get(0).resourceId()).isEqualTo(resource2);
    assertThat(remaining.get(0).permission()).isEqualTo(Permission.NAMESPACE_READ);
  }

  @Test
  void deleteShouldNotThrowWhenEntryMissing() {
    assertThatCode(
            () ->
                accessControlEntryRepository.delete(
                    ctx,
                    GranteeType.USER,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    Permission.CATALOG_READ))
        .doesNotThrowAnyException();
  }
}
