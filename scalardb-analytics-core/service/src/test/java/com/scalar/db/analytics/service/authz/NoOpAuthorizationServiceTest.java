/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.authz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.domain.authz.ResourceRef;
import com.scalar.db.analytics.domain.authz.ResourceType;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NoOpAuthorizationServiceTest {

  private final NoOpAuthorizationService service = new NoOpAuthorizationService();

  @Test
  void authorizeSuperAdmin_shouldAlwaysPass() {
    assertThatCode(() -> service.authorizeSuperAdmin(UUID.randomUUID())).doesNotThrowAnyException();
  }

  @Test
  void authorizePermissionManagement_shouldAlwaysPass() {
    assertThatCode(
            () -> service.authorizePermissionManagement(UUID.randomUUID(), UUID.randomUUID()))
        .doesNotThrowAnyException();
  }

  @Test
  void authorize_shouldAlwaysPass() {
    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(
                new ResourceRef(ResourceType.CATALOG, UUID.randomUUID()),
                Set.of(Permission.CATALOG_ADMIN)));

    assertThatCode(() -> service.authorize(UUID.randomUUID(), requirements))
        .doesNotThrowAnyException();
  }

  @Test
  void filterAuthorized_shouldReturnAllItems() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    List<UUID> items = List.of(id1, id2);

    List<UUID> result =
        service.filterAuthorized(
            UUID.randomUUID(),
            items,
            id ->
                List.of(
                    new PermissionRequirement(
                        new ResourceRef(ResourceType.CATALOG, id),
                        Set.of(Permission.CATALOG_READ))));

    assertThat(result).containsExactly(id1, id2);
  }
}
