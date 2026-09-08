/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.authz;

import static java.util.Objects.requireNonNull;

import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider;
import com.scalar.db.analytics.domain.authz.AccessControlEntry;
import com.scalar.db.analytics.domain.authz.GranteeType;
import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.domain.authz.ResourceRef;
import com.scalar.db.analytics.domain.authz.ResourceType;
import com.scalar.db.analytics.domain.authz.Role;
import com.scalar.db.analytics.domain.authz.RoleAssignment;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.authz.AccessControlEntryRepository;
import com.scalar.db.analytics.repository.authz.RoleAssignmentRepository;
import com.scalar.db.analytics.repository.authz.RoleRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link AuthorizationService}.
 *
 * <p>Performs authorization checks by resolving the user's roles, collecting ACEs (both direct user
 * grants and role-based grants), and matching them against operation-declared permission
 * requirements. Users assigned to the built-in SUPERADMIN role bypass all checks.
 *
 * <p>When a {@link ScalarDbAuthorizationDelegate} is configured, authorization for ScalarDB data
 * source resources (Namespace, Table) is delegated to ScalarDB Cluster's privilege system instead
 * of Analytics ACL. The two authorization paths are mutually exclusive per resource.
 *
 * @param <T> the type of transaction context
 */
public class AuthorizationServiceImpl<T extends RepositoryTransactionContext>
    implements AuthorizationService {

  private static final Logger logger = LoggerFactory.getLogger(AuthorizationServiceImpl.class);

  /** The reserved name of the built-in SUPERADMIN role. */
  public static final String SUPERADMIN_ROLE_NAME = "SUPERADMIN";

  private final RoleRepository<T> roleRepository;
  private final RoleAssignmentRepository<T> roleAssignmentRepository;
  private final AccessControlEntryRepository<T> aceRepository;
  private final RepositoryTransactionManager<T> txManager;
  @Nullable private final ScalarDbAuthorizationDelegate scalarDbDelegate;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public AuthorizationServiceImpl(
      RoleRepository<T> roleRepository,
      RoleAssignmentRepository<T> roleAssignmentRepository,
      AccessControlEntryRepository<T> aceRepository,
      RepositoryTransactionManager<T> txManager) {
    this(roleRepository, roleAssignmentRepository, aceRepository, txManager, null);
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public AuthorizationServiceImpl(
      RoleRepository<T> roleRepository,
      RoleAssignmentRepository<T> roleAssignmentRepository,
      AccessControlEntryRepository<T> aceRepository,
      RepositoryTransactionManager<T> txManager,
      @Nullable ScalarDbAuthorizationDelegate scalarDbDelegate) {
    this.roleRepository = roleRepository;
    this.roleAssignmentRepository = roleAssignmentRepository;
    this.aceRepository = aceRepository;
    this.txManager = txManager;
    this.scalarDbDelegate = scalarDbDelegate;
  }

  @Override
  public boolean authorizeSuperAdmin(UUID userId) {
    return txManager.withTransaction(
        ctx -> {
          List<RoleAssignment> assignments = roleAssignmentRepository.findByUserId(ctx, userId);
          if (!isSuperAdmin(ctx, assignments)) {
            logger.warn("SUPERADMIN authorization denied for user: {}", userId);
            return false;
          }
          return true;
        });
  }

  @Override
  public boolean authorize(UUID userId, List<PermissionRequirement> requirements) {
    if (requirements.isEmpty()) {
      return true;
    }

    // 1. SUPERADMIN bypass
    if (checkSuperAdmin(userId)) {
      return true;
    }

    // 2. Check each requirement via delegation or ACL (OR semantics)
    List<AccessControlEntry> aces = null; // lazy-loaded
    for (PermissionRequirement req : requirements) {
      if (isScalarDbDelegationTarget(req.resource())) {
        // ScalarDB delegation for this resource. isScalarDbDelegationTarget guarantees a non-null
        // delegate.
        if (requireNonNull(scalarDbDelegate).authorize(userId, List.of(req))) {
          return true;
        }
      } else {
        // Analytics ACL for this resource
        if (aces == null) {
          aces = loadAces(userId);
        }
        if (anyRequirementSatisfied(aces, List.of(req))) {
          return true;
        }
      }
    }

    logger.warn(
        "Authorization denied for user {}: none of {} requirement(s) satisfied: {}",
        userId,
        requirements.size(),
        requirements);
    return false;
  }

  @Override
  public boolean authorizePermissionManagement(UUID userId, UUID catalogId) {
    return txManager.withTransaction(
        ctx -> {
          List<RoleAssignment> assignments = roleAssignmentRepository.findByUserId(ctx, userId);
          if (isSuperAdmin(ctx, assignments)) {
            return true;
          }
          List<AccessControlEntry> aces = collectAces(ctx, userId, assignments);
          for (AccessControlEntry ace : aces) {
            if (ace.resourceId().equals(catalogId)
                && ace.permission() == Permission.CATALOG_ADMIN) {
              return true;
            }
          }
          logger.warn(
              "Permission management authorization denied for user {} on catalog {}",
              userId,
              catalogId);
          return false;
        });
  }

  @Override
  public <R> List<R> filterAuthorized(
      UUID userId, List<R> items, Function<R, List<PermissionRequirement>> requirementsMapper) {
    if (items.isEmpty()) {
      return List.of();
    }

    // 1. SUPERADMIN bypass
    if (checkSuperAdmin(userId)) {
      return List.copyOf(items);
    }

    // 2. Filter each item: check requirements via delegation or ACL (OR semantics)
    List<AccessControlEntry> aces = loadAces(userId);
    return items.stream()
        .filter(
            item -> {
              List<PermissionRequirement> reqs = requirementsMapper.apply(item);
              if (reqs.isEmpty()) {
                return true;
              }
              for (PermissionRequirement req : reqs) {
                if (isScalarDbDelegationTarget(req.resource())) {
                  // isScalarDbDelegationTarget guarantees a non-null delegate.
                  if (requireNonNull(scalarDbDelegate).authorize(userId, List.of(req))) {
                    return true;
                  }
                } else {
                  if (anyRequirementSatisfied(aces, List.of(req))) {
                    return true;
                  }
                }
              }
              return false;
            })
        .toList();
  }

  private boolean checkSuperAdmin(UUID userId) {
    return txManager.withTransaction(
        ctx -> {
          List<RoleAssignment> assignments = roleAssignmentRepository.findByUserId(ctx, userId);
          return isSuperAdmin(ctx, assignments);
        });
  }

  private List<AccessControlEntry> loadAces(UUID userId) {
    return txManager.withTransaction(
        ctx -> {
          List<RoleAssignment> assignments = roleAssignmentRepository.findByUserId(ctx, userId);
          return collectAces(ctx, userId, assignments);
        });
  }

  private boolean isSuperAdmin(T ctx, List<RoleAssignment> assignments) {
    Optional<Role> superAdminRole = roleRepository.findByName(ctx, SUPERADMIN_ROLE_NAME);
    if (superAdminRole.isPresent() && superAdminRole.get().builtIn()) {
      UUID superAdminId = superAdminRole.get().id();
      return assignments.stream().anyMatch(a -> a.roleId().equals(superAdminId));
    }
    return false;
  }

  private List<AccessControlEntry> collectAces(
      T ctx, UUID userId, List<RoleAssignment> assignments) {
    List<AccessControlEntry> allAces =
        new ArrayList<>(aceRepository.findByGrantee(ctx, GranteeType.USER, userId));
    for (RoleAssignment assignment : assignments) {
      allAces.addAll(aceRepository.findByGrantee(ctx, GranteeType.ROLE, assignment.roleId()));
    }
    return allAces;
  }

  private boolean isScalarDbDelegationTarget(ResourceRef ref) {
    if (scalarDbDelegate == null) {
      return false;
    }
    return (ref.resourceType() == ResourceType.NAMESPACE
            || ref.resourceType() == ResourceType.TABLE)
        && ScalarDbProvider.TYPE.equals(ref.dataSourceProviderType());
  }

  private static boolean anyRequirementSatisfied(
      List<AccessControlEntry> aces, List<PermissionRequirement> requirements) {
    for (PermissionRequirement req : requirements) {
      UUID targetResourceId = req.resource().resourceId();
      for (AccessControlEntry ace : aces) {
        if (ace.resourceId().equals(targetResourceId)
            && req.satisfyingPermissions().contains(ace.permission())) {
          return true;
        }
      }
    }
    return false;
  }
}
