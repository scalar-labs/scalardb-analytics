/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.authz;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.api.auth.AccessToken;
import com.scalar.db.analytics.api.auth.PasswordCredential;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.authz.GranteeType;
import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.domain.authz.ResourceRef;
import com.scalar.db.analytics.domain.authz.ResourceType;
import com.scalar.db.analytics.server.support.UseCaseIntegrationTestBase;
import com.scalar.db.analytics.service.authz.AuthorizationService;
import com.scalar.db.analytics.service.authz.PermissionRequirement;
import com.scalar.db.analytics.usecase.CatalogUseCase;
import com.scalar.db.analytics.usecase.auth.UserUseCase;
import com.scalar.db.analytics.usecase.authz.PermissionUseCase;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuthorizationServiceIntegrationTest extends UseCaseIntegrationTestBase {

  @Autowired private AuthorizationService authorizationService;
  @Autowired private CatalogUseCase catalogUseCase;
  @Autowired private PermissionUseCase permissionUseCase;

  private static UUID testCatalogId;
  private static UUID testCatalogId2;
  private static UUID aliceUserId;

  @BeforeAll
  static void setUpTestData(
      @Autowired UserUseCase userUseCase,
      @Autowired CatalogUseCase catalogUseCase,
      @Autowired com.scalar.db.analytics.usecase.auth.AuthenticationUseCase authenticationUseCase) {
    AccessToken adminToken =
        authenticationUseCase.authenticateWithPassword(
            new PasswordCredential(ADMIN_USERNAME, ADMIN_PASSWORD));
    UUID adminId = UUID.fromString(adminToken.getUserId());

    var catalog = catalogUseCase.createCatalog(adminId, "authz-test-catalog");
    testCatalogId = catalog.getId();

    var catalog2 = catalogUseCase.createCatalog(adminId, "authz-test-catalog-2");
    testCatalogId2 = catalog2.getId();

    aliceUserId =
        userUseCase
            .createUserWithBackendUser(adminId, "alice-authz", "alice-authz", "alice-pass")
            .getUserId();
  }

  @AfterAll
  static void cleanUpTestData(
      @Autowired UserUseCase userUseCase,
      @Autowired CatalogUseCase catalogUseCase,
      @Autowired com.scalar.db.analytics.usecase.auth.AuthenticationUseCase authenticationUseCase) {
    AccessToken adminToken =
        authenticationUseCase.authenticateWithPassword(
            new PasswordCredential(ADMIN_USERNAME, ADMIN_PASSWORD));
    UUID adminId = UUID.fromString(adminToken.getUserId());

    try {
      userUseCase.deleteUser(adminId, "alice-authz", true);
    } catch (AnalyticsException ignored) {
    }
    try {
      catalogUseCase.deleteCatalog(adminId, "authz-test-catalog", true);
    } catch (AnalyticsException ignored) {
    }
    try {
      catalogUseCase.deleteCatalog(adminId, "authz-test-catalog-2", true);
    } catch (AnalyticsException ignored) {
    }
  }

  @Test
  void superadminBypass_shouldReturnTrue() {
    UUID adminId = adminUserId();
    assertThat(authorizationService.authorizeSuperAdmin(adminId)).isTrue();
  }

  @Test
  void nonSuperadmin_authorizeSuperAdmin_shouldReturnFalse() {
    assertThat(authorizationService.authorizeSuperAdmin(aliceUserId)).isFalse();
  }

  @Test
  void directPermissionCheck_shouldWork() {
    UUID adminId = adminUserId();

    permissionUseCase.grantCatalogPermission(
        adminId, GranteeType.USER, "alice-authz", Permission.CATALOG_READ, "authz-test-catalog");

    try {
      ResourceRef catalogRef = new ResourceRef(ResourceType.CATALOG, testCatalogId);
      boolean authorized =
          authorizationService.authorize(
              aliceUserId,
              List.of(new PermissionRequirement(catalogRef, Set.of(Permission.CATALOG_READ))));
      assertThat(authorized).isTrue();

      boolean notAuthorized =
          authorizationService.authorize(
              aliceUserId,
              List.of(new PermissionRequirement(catalogRef, Set.of(Permission.CATALOG_WRITE))));
      assertThat(notAuthorized).isFalse();
    } finally {
      permissionUseCase.revokeCatalogPermission(
          adminId, GranteeType.USER, "alice-authz", Permission.CATALOG_READ, "authz-test-catalog");
    }
  }

  @Test
  void filterAuthorized_shouldFilterByPermission() {
    UUID adminId = adminUserId();

    // The requirements mapper derives the resource from each item so that filterAuthorized is
    // exercised per resource, not against a single fixed catalog.
    Function<UUID, List<PermissionRequirement>> requirementsMapper =
        id ->
            List.of(
                new PermissionRequirement(
                    new ResourceRef(ResourceType.CATALOG, id), Set.of(Permission.CATALOG_READ)));

    // Grant alice CATALOG_READ on only the first catalog
    permissionUseCase.grantCatalogPermission(
        adminId, GranteeType.USER, "alice-authz", Permission.CATALOG_READ, "authz-test-catalog");

    try {
      // alice should get only the catalog she has permission on, filtering out the other one
      var filtered =
          authorizationService.filterAuthorized(
              aliceUserId, List.of(testCatalogId, testCatalogId2), requirementsMapper);

      assertThat(filtered).containsExactly(testCatalogId);

      // SUPERADMIN should get all items
      var adminFiltered =
          authorizationService.filterAuthorized(
              adminId, List.of(testCatalogId, testCatalogId2), requirementsMapper);

      assertThat(adminFiltered).containsExactly(testCatalogId, testCatalogId2);
    } finally {
      permissionUseCase.revokeCatalogPermission(
          adminId, GranteeType.USER, "alice-authz", Permission.CATALOG_READ, "authz-test-catalog");
    }
  }
}
