/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.auth.AuthUser;
import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.api.auth.UserDetail;
import com.scalar.db.analytics.api.auth.UserInfo;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.auth.PasswordIdentity;
import com.scalar.db.analytics.domain.auth.internal.InternalCredential;
import com.scalar.db.analytics.domain.authz.AccessControlEntry;
import com.scalar.db.analytics.domain.authz.GranteeType;
import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.domain.authz.Role;
import com.scalar.db.analytics.domain.authz.RoleAssignment;
import com.scalar.db.analytics.lib.functional.ThrowableFunction;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.auth.AccessTokenRepository;
import com.scalar.db.analytics.repository.auth.AuthUserRepository;
import com.scalar.db.analytics.repository.auth.PasswordIdentityRepository;
import com.scalar.db.analytics.repository.auth.internal.InternalCredentialRepository;
import com.scalar.db.analytics.repository.authz.AccessControlEntryRepository;
import com.scalar.db.analytics.repository.authz.RoleAssignmentRepository;
import com.scalar.db.analytics.repository.authz.RoleRepository;
import com.scalar.db.analytics.service.authz.AuthorizationService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserUseCaseImplTest {

  private static final UUID USER_ID = UUID.randomUUID();

  @Mock private PasswordIdentityRepository<RepositoryTransactionContext> passwordIdentityRepository;
  @Mock private AuthUserRepository<RepositoryTransactionContext> authUserRepository;
  @Mock private RoleAssignmentRepository<RepositoryTransactionContext> roleAssignmentRepository;
  @Mock private RoleRepository<RepositoryTransactionContext> roleRepository;

  @Mock
  private InternalCredentialRepository<RepositoryTransactionContext> internalCredentialRepository;

  @Mock private AccessControlEntryRepository<RepositoryTransactionContext> aceRepository;
  @Mock private AccessTokenRepository<RepositoryTransactionContext> accessTokenRepository;
  @Mock private RepositoryTransactionContext txContext;
  @Mock private RepositoryTransactionManager<RepositoryTransactionContext> txManager;
  @Mock private AuthorizationService authorizationService;
  @Mock private PasswordEncoder passwordEncoder;

  private UserUseCaseImpl<RepositoryTransactionContext> useCase;

  @BeforeEach
  void setUp() throws Exception {
    useCase =
        new UserUseCaseImpl<>(
            passwordIdentityRepository,
            authUserRepository,
            roleAssignmentRepository,
            roleRepository,
            internalCredentialRepository,
            aceRepository,
            accessTokenRepository,
            txManager,
            authorizationService,
            passwordEncoder);

    lenient().when(authorizationService.authorizeSuperAdmin(any())).thenReturn(true);
    lenient()
        .when(txManager.withTransaction(any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<RepositoryTransactionContext, ?, Exception> function =
                  invocation.getArgument(0);
              return function.apply(txContext);
            });
  }

  @Test
  void listUsers_shouldListPrincipalsIncludingThoseWithoutABackendIdentity() throws Exception {
    // alice has an INTERNAL identity; bob has a SCALARDB_CLUSTER identity; carol is a principal
    // created without any backend user, so it has no identity and must still be listed.
    UUID aliceId = UUID.randomUUID();
    UUID bobId = UUID.randomUUID();
    UUID carolId = UUID.randomUUID();

    when(authUserRepository.findAll(txContext))
        .thenReturn(
            List.of(
                new AuthUser(aliceId, "alice"),
                new AuthUser(bobId, "bob"),
                new AuthUser(carolId, "carol")));
    when(passwordIdentityRepository.findByUserId(txContext, aliceId))
        .thenReturn(
            List.of(
                new PasswordIdentity(
                    UUID.randomUUID(), aliceId, PasswordBackendType.INTERNAL, "alice-internal")));
    when(passwordIdentityRepository.findByUserId(txContext, bobId))
        .thenReturn(
            List.of(
                new PasswordIdentity(
                    UUID.randomUUID(),
                    bobId,
                    PasswordBackendType.SCALARDB_CLUSTER,
                    "bob-cluster")));
    when(passwordIdentityRepository.findByUserId(txContext, carolId)).thenReturn(List.of());

    List<UserInfo> result = useCase.listUsers(USER_ID);

    assertThat(result)
        .extracting(UserInfo::getUsername)
        .containsExactlyInAnyOrder("alice", "bob", "carol");
    UserInfo carol =
        result.stream().filter(u -> u.getUsername().equals("carol")).findFirst().orElseThrow();
    assertThat(carol.getBackend()).isNull();
    assertThat(carol.getBackendUserId()).isNull();
    UserInfo alice =
        result.stream().filter(u -> u.getUsername().equals("alice")).findFirst().orElseThrow();
    assertThat(alice.getBackend()).isEqualTo(PasswordBackendType.INTERNAL);
    assertThat(alice.getBackendUserId()).isEqualTo("alice-internal");
    verify(authorizationService).authorizeSuperAdmin(USER_ID);
  }

  @Test
  void listUsers_whenAccessDenied_shouldThrowException() throws Exception {
    when(authorizationService.authorizeSuperAdmin(USER_ID)).thenReturn(false);

    assertThatThrownBy(() -> useCase.listUsers(USER_ID)).isInstanceOf(AnalyticsException.class);

    verify(authUserRepository, never()).findAll(any());
  }

  @Test
  void createUser_shouldCreatePrincipalWhenUsernameAvailable() throws Exception {
    when(authUserRepository.findByUsername(txContext, "alice")).thenReturn(Optional.empty());

    AuthUser result = useCase.createUser(USER_ID, "alice");

    assertThat(result.getUsername()).isEqualTo("alice");
    verify(authUserRepository).create(eq(txContext), any(AuthUser.class));
  }

  @Test
  void createUser_shouldThrowWhenUsernameAlreadyExists() throws Exception {
    when(authUserRepository.findByUsername(txContext, "alice"))
        .thenReturn(Optional.of(new AuthUser(UUID.randomUUID(), "alice")));

    assertThatThrownBy(() -> useCase.createUser(USER_ID, "alice"))
        .isInstanceOf(AnalyticsException.class)
        .extracting(ex -> ((AnalyticsException) ex).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.USER_ALREADY_EXISTS);

    verify(authUserRepository, never()).create(any(), any());
  }

  @Test
  void createUserWithBackendUser_shouldCreatePrincipalCredentialAndIdentity() throws Exception {
    when(authUserRepository.findByUsername(txContext, "alice")).thenReturn(Optional.empty());
    when(internalCredentialRepository.findByUsername(txContext, "alice-internal"))
        .thenReturn(Optional.empty());
    when(passwordEncoder.encode("pw")).thenReturn("hash");

    AuthUser result = useCase.createUserWithBackendUser(USER_ID, "alice", "alice-internal", "pw");

    assertThat(result.getUsername()).isEqualTo("alice");
    verify(authUserRepository).create(eq(txContext), any(AuthUser.class));
    verify(internalCredentialRepository).create(eq(txContext), any(InternalCredential.class));
    verify(passwordIdentityRepository).create(eq(txContext), any(PasswordIdentity.class));
  }

  @Test
  void createUserWithBackendUser_shouldThrowWhenBackendUserAlreadyExists() throws Exception {
    when(authUserRepository.findByUsername(txContext, "alice")).thenReturn(Optional.empty());
    when(internalCredentialRepository.findByUsername(txContext, "alice-internal"))
        .thenReturn(Optional.of(new InternalCredential("alice-internal", "hash")));

    assertThatThrownBy(
            () -> useCase.createUserWithBackendUser(USER_ID, "alice", "alice-internal", "pw"))
        .isInstanceOf(AnalyticsException.class)
        .extracting(ex -> ((AnalyticsException) ex).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.USER_ALREADY_EXISTS);

    verify(authUserRepository, never()).create(any(), any());
    verify(passwordIdentityRepository, never()).create(any(), any());
  }

  @Test
  void deleteUserById_shouldReturnFalseWhenPrincipalMissing() throws Exception {
    UUID targetId = UUID.randomUUID();
    when(authUserRepository.findById(txContext, targetId)).thenReturn(Optional.empty());

    assertThat(useCase.deleteUserById(USER_ID, targetId, false)).isFalse();

    verify(authUserRepository, never()).deleteById(any(), any());
  }

  @Test
  void deleteUserById_withoutCascade_shouldDeletePrincipalWhenNoDependents() throws Exception {
    UUID targetId = UUID.randomUUID();
    when(authUserRepository.findById(txContext, targetId))
        .thenReturn(Optional.of(new AuthUser(targetId, "alice")));
    when(passwordIdentityRepository.findByUserId(txContext, targetId)).thenReturn(List.of());
    when(roleAssignmentRepository.findByUserId(txContext, targetId)).thenReturn(List.of());
    when(aceRepository.findByGrantee(txContext, GranteeType.USER, targetId)).thenReturn(List.of());

    assertThat(useCase.deleteUserById(USER_ID, targetId, false)).isTrue();

    verify(accessTokenRepository).deleteByUserId(txContext, targetId);
    verify(authUserRepository).deleteById(txContext, targetId);
  }

  @Test
  void describeUserById_shouldReturnUserDetailWithRoles() throws Exception {
    UUID targetUserId = UUID.randomUUID();
    UUID roleId = UUID.randomUUID();
    AuthUser authUser = new AuthUser(targetUserId, "alice");
    PasswordIdentity identity =
        new PasswordIdentity(
            UUID.randomUUID(), targetUserId, PasswordBackendType.INTERNAL, "alice");
    RoleAssignment assignment = new RoleAssignment(targetUserId, roleId);
    Role role = new Role(roleId, "admin", false);

    when(authUserRepository.findById(txContext, targetUserId)).thenReturn(Optional.of(authUser));
    when(passwordIdentityRepository.findByUserId(txContext, targetUserId))
        .thenReturn(List.of(identity));
    when(roleAssignmentRepository.findByUserId(txContext, targetUserId))
        .thenReturn(List.of(assignment));
    when(roleRepository.findById(txContext, roleId)).thenReturn(Optional.of(role));

    Optional<UserDetail> result = useCase.describeUserById(USER_ID, targetUserId);

    assertThat(result).isPresent();
    UserDetail detail = result.get();
    assertThat(detail.getUserId()).isEqualTo(targetUserId.toString());
    assertThat(detail.getBackendUserId()).isEqualTo("alice");
    assertThat(detail.getBackend()).isEqualTo(PasswordBackendType.INTERNAL);
    assertThat(detail.getRoles()).containsExactly("admin");
    assertThat(detail.getUsername()).isEqualTo("alice");
  }

  @Test
  void describeUserById_shouldReturnEmptyWhenUserNotFound() throws Exception {
    UUID targetUserId = UUID.randomUUID();
    when(authUserRepository.findById(txContext, targetUserId)).thenReturn(Optional.empty());

    Optional<UserDetail> result = useCase.describeUserById(USER_ID, targetUserId);

    assertThat(result).isEmpty();
  }

  @Test
  void describeUserById_whenAccessDenied_shouldThrowException() throws Exception {
    when(authorizationService.authorizeSuperAdmin(USER_ID)).thenReturn(false);

    assertThatThrownBy(() -> useCase.describeUserById(USER_ID, UUID.randomUUID()))
        .isInstanceOf(AnalyticsException.class);

    verify(authUserRepository, never()).findById(any(), any());
  }

  @Test
  void describeUserByName_shouldReturnUserDetailWithRoles() throws Exception {
    UUID targetUserId = UUID.randomUUID();
    UUID roleId = UUID.randomUUID();
    AuthUser authUser = new AuthUser(targetUserId, "alice");
    PasswordIdentity identity =
        new PasswordIdentity(
            UUID.randomUUID(), targetUserId, PasswordBackendType.INTERNAL, "alice");
    RoleAssignment assignment = new RoleAssignment(targetUserId, roleId);
    Role role = new Role(roleId, "admin", false);

    when(authUserRepository.findByUsername(txContext, "alice")).thenReturn(Optional.of(authUser));
    when(passwordIdentityRepository.findByUserId(txContext, targetUserId))
        .thenReturn(List.of(identity));
    when(roleAssignmentRepository.findByUserId(txContext, targetUserId))
        .thenReturn(List.of(assignment));
    when(roleRepository.findById(txContext, roleId)).thenReturn(Optional.of(role));

    Optional<UserDetail> result = useCase.describeUserByName(USER_ID, "alice");

    assertThat(result).isPresent();
    UserDetail detail = result.get();
    assertThat(detail.getUserId()).isEqualTo(targetUserId.toString());
    assertThat(detail.getBackendUserId()).isEqualTo("alice");
    assertThat(detail.getBackend()).isEqualTo(PasswordBackendType.INTERNAL);
    assertThat(detail.getRoles()).containsExactly("admin");
    assertThat(detail.getUsername()).isEqualTo("alice");
  }

  @Test
  void describeUserByName_shouldReturnEmptyWhenUserNotFound() throws Exception {
    when(authUserRepository.findByUsername(txContext, "nonexistent")).thenReturn(Optional.empty());

    Optional<UserDetail> result = useCase.describeUserByName(USER_ID, "nonexistent");

    assertThat(result).isEmpty();
  }

  @Test
  void describeUserByName_whenAccessDenied_shouldThrowException() throws Exception {
    when(authorizationService.authorizeSuperAdmin(USER_ID)).thenReturn(false);

    assertThatThrownBy(() -> useCase.describeUserByName(USER_ID, "alice"))
        .isInstanceOf(AnalyticsException.class);

    verify(authUserRepository, never()).findByUsername(any(), any());
  }

  @Test
  void deleteUser_withoutCascade_shouldBlockWhenDirectAcesExist() throws Exception {
    // A principal with no identities, no role assignments, but with a direct USER-grantee ACE.
    // Without cascade, the delete must refuse so the admin gets a safety-net warning that they
    // are about to wipe explicit permission grants — symmetric with the existing block on role
    // assignments. Recovery: `permission revoke ... --user <u>` per ACE, or `--cascade`.
    UUID aliceUserId = UUID.randomUUID();
    AuthUser alice = new AuthUser(aliceUserId, "alice");
    UUID resourceId = UUID.randomUUID();
    AccessControlEntry directAce =
        AccessControlEntry.create(
            GranteeType.USER, aliceUserId, resourceId, Permission.CATALOG_READ);

    when(authUserRepository.findByUsername(txContext, "alice")).thenReturn(Optional.of(alice));
    when(passwordIdentityRepository.findByUserId(txContext, aliceUserId)).thenReturn(List.of());
    when(roleAssignmentRepository.findByUserId(txContext, aliceUserId)).thenReturn(List.of());
    when(aceRepository.findByGrantee(txContext, GranteeType.USER, aliceUserId))
        .thenReturn(List.of(directAce));

    assertThatThrownBy(() -> useCase.deleteUser(USER_ID, "alice", false))
        .isInstanceOf(AnalyticsException.class)
        .extracting(ex -> ((AnalyticsException) ex).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.USER_NOT_EMPTY);

    // Nothing was deleted.
    verify(authUserRepository, never()).deleteById(any(), any());
    verify(accessTokenRepository, never()).deleteByUserId(any(), any());
  }

  @Test
  void deleteUser_withoutCascade_shouldCleanUpAccessTokensWhenNoOtherDependents() throws Exception {
    // No identities, no role assignments, no direct ACEs. The principal can still have token
    // rows (e.g. from a recent login); these are invisible to the admin and meaningless once the
    // principal is gone. The non-cascade path must clean them up so the principal-row delete
    // does not leave orphan tokens pointing at a vanished userId.
    UUID aliceUserId = UUID.randomUUID();
    AuthUser alice = new AuthUser(aliceUserId, "alice");

    when(authUserRepository.findByUsername(txContext, "alice")).thenReturn(Optional.of(alice));
    when(passwordIdentityRepository.findByUserId(txContext, aliceUserId)).thenReturn(List.of());
    when(roleAssignmentRepository.findByUserId(txContext, aliceUserId)).thenReturn(List.of());
    when(aceRepository.findByGrantee(txContext, GranteeType.USER, aliceUserId))
        .thenReturn(List.of());

    boolean deleted = useCase.deleteUser(USER_ID, "alice", false);

    assertThat(deleted).isTrue();
    verify(accessTokenRepository).deleteByUserId(txContext, aliceUserId);
    verify(authUserRepository).deleteById(txContext, aliceUserId);
    // Non-cascade path must not touch ACE / role assignment / identity / internal-credential
    // repositories beyond the guard reads above.
    verify(aceRepository, never()).deleteByGrantee(any(), any(), any());
    verify(roleAssignmentRepository, never()).deleteByUserId(any(), any());
    verify(passwordIdentityRepository, never()).deleteByUserId(any(), any());
    verify(passwordIdentityRepository, never()).deleteByUserIdAndBackend(any(), any(), any());
  }

  @Test
  void deleteUser_withCascade_shouldDeleteInternalCredentialAndAllAuxiliaries() throws Exception {
    // Cascade-deleting a principal that has an INTERNAL identity, a role assignment, and a direct
    // USER-grantee ACE must tear everything down: the internal-backend credential, all identities,
    // the auxiliary records (ACE / role assignment / access tokens), and finally the principal row.
    // This pins the selective-deletion logic that integration tests cover but unit tests did not.
    UUID aliceUserId = UUID.randomUUID();
    AuthUser alice = new AuthUser(aliceUserId, "alice");
    PasswordIdentity internalIdentity =
        new PasswordIdentity(
            UUID.randomUUID(), aliceUserId, PasswordBackendType.INTERNAL, "alice-internal");
    RoleAssignment assignment = new RoleAssignment(aliceUserId, UUID.randomUUID());
    AccessControlEntry directAce =
        AccessControlEntry.create(
            GranteeType.USER, aliceUserId, UUID.randomUUID(), Permission.CATALOG_READ);

    when(authUserRepository.findByUsername(txContext, "alice")).thenReturn(Optional.of(alice));
    when(passwordIdentityRepository.findByUserId(txContext, aliceUserId))
        .thenReturn(List.of(internalIdentity));
    when(roleAssignmentRepository.findByUserId(txContext, aliceUserId))
        .thenReturn(List.of(assignment));
    when(aceRepository.findByGrantee(txContext, GranteeType.USER, aliceUserId))
        .thenReturn(List.of(directAce));

    assertThat(useCase.deleteUser(USER_ID, "alice", true)).isTrue();

    // INTERNAL identity → the internal-backend credential is removed.
    verify(internalCredentialRepository).deleteByUsername(txContext, "alice-internal");
    // All identities are cleared.
    verify(passwordIdentityRepository).deleteByUserId(txContext, aliceUserId);
    // deleteUserAuxiliaries: ACE + role assignment + access tokens.
    verify(aceRepository).deleteByGrantee(txContext, GranteeType.USER, aliceUserId);
    verify(roleAssignmentRepository).deleteByUserId(txContext, aliceUserId);
    verify(accessTokenRepository).deleteByUserId(txContext, aliceUserId);
    // deletePrincipalRow.
    verify(authUserRepository).deleteById(txContext, aliceUserId);
  }

  @Test
  void deleteUser_withCascade_shouldNotDeleteInternalCredentialForNonInternalIdentity()
      throws Exception {
    // The over-delete safeguard: when a principal's only identity is on a non-INTERNAL backend
    // (SCALARDB_CLUSTER), cascade delete must NOT call
    // internalCredentialRepository.deleteByUsername — there is no internal credential to remove,
    // and deleting one keyed by the cluster backendUserId would wipe an unrelated internal user.
    // The principal and its identities are still removed.
    UUID bobUserId = UUID.randomUUID();
    AuthUser bob = new AuthUser(bobUserId, "bob");
    PasswordIdentity clusterIdentity =
        new PasswordIdentity(
            UUID.randomUUID(), bobUserId, PasswordBackendType.SCALARDB_CLUSTER, "bob-cluster");

    when(authUserRepository.findByUsername(txContext, "bob")).thenReturn(Optional.of(bob));
    when(passwordIdentityRepository.findByUserId(txContext, bobUserId))
        .thenReturn(List.of(clusterIdentity));
    when(roleAssignmentRepository.findByUserId(txContext, bobUserId)).thenReturn(List.of());
    when(aceRepository.findByGrantee(txContext, GranteeType.USER, bobUserId)).thenReturn(List.of());

    assertThat(useCase.deleteUser(USER_ID, "bob", true)).isTrue();

    // The INTERNAL guard prevents touching internal credentials for a cluster-only principal.
    verify(internalCredentialRepository, never()).deleteByUsername(any(), any());
    // Identities and the principal row are still removed.
    verify(passwordIdentityRepository).deleteByUserId(txContext, bobUserId);
    verify(authUserRepository).deleteById(txContext, bobUserId);
  }

  @Test
  void unlinkBackendUser_shouldDeleteOnlyMatchingBackendIdentity() throws Exception {
    // A principal that holds identities on both INTERNAL and SCALARDB_CLUSTER; dissociating the
    // INTERNAL link must remove only the INTERNAL identity, leaving SCALARDB_CLUSTER intact.
    UUID aliceUserId = UUID.randomUUID();
    AuthUser alice = new AuthUser(aliceUserId, "alice");
    PasswordIdentity internalIdentity =
        new PasswordIdentity(
            UUID.randomUUID(), aliceUserId, PasswordBackendType.INTERNAL, "alice-internal");

    when(authUserRepository.findByUsername(txContext, "alice")).thenReturn(Optional.of(alice));
    when(passwordIdentityRepository.findByBackendAndBackendUserId(
            txContext, PasswordBackendType.INTERNAL, "alice-internal"))
        .thenReturn(Optional.of(internalIdentity));

    useCase.unlinkBackendUser(USER_ID, "alice", "alice-internal", PasswordBackendType.INTERNAL);

    verify(passwordIdentityRepository)
        .deleteByUserIdAndBackend(txContext, aliceUserId, PasswordBackendType.INTERNAL);
    verify(passwordIdentityRepository, never())
        .deleteByUserIdAndBackend(any(), eq(aliceUserId), eq(PasswordBackendType.SCALARDB_CLUSTER));
    verify(passwordIdentityRepository, never()).deleteByUserId(any(), any());
    // Unlinking revokes the principal's access tokens on the safe side.
    verify(accessTokenRepository).deleteByUserId(txContext, aliceUserId);
  }

  @Test
  void linkBackendUser_shouldCreateIdentityForScalarDbClusterBackend() throws Exception {
    // For a non-INTERNAL backend, the use case must NOT consult internalCredentialRepository;
    // pre-provisioning a principal that a future cluster user will JIT-link to is a legitimate
    // use case and there is no internal-credential row to check.
    UUID aliceUserId = UUID.randomUUID();
    AuthUser alice = new AuthUser(aliceUserId, "alice");

    when(authUserRepository.findByUsername(txContext, "alice")).thenReturn(Optional.of(alice));
    when(passwordIdentityRepository.findByBackendAndBackendUserId(
            txContext, PasswordBackendType.SCALARDB_CLUSTER, "alice-cluster"))
        .thenReturn(Optional.empty());
    when(passwordIdentityRepository.findByUserId(txContext, aliceUserId)).thenReturn(List.of());

    useCase.linkBackendUser(
        USER_ID, "alice", "alice-cluster", PasswordBackendType.SCALARDB_CLUSTER);

    ArgumentCaptor<PasswordIdentity> captor = ArgumentCaptor.forClass(PasswordIdentity.class);
    verify(passwordIdentityRepository).create(eq(txContext), captor.capture());
    PasswordIdentity created = captor.getValue();
    assertThat(created.backend()).isEqualTo(PasswordBackendType.SCALARDB_CLUSTER);
    assertThat(created.backendUserId()).isEqualTo("alice-cluster");
    assertThat(created.userId()).isEqualTo(aliceUserId);
    verify(internalCredentialRepository, never()).findByUsername(any(), any());
  }
}
