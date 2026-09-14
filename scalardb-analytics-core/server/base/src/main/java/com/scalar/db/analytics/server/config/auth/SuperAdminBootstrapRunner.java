/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.config.auth;

import com.scalar.db.analytics.api.auth.AuthUser;
import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.domain.auth.PasswordIdentity;
import com.scalar.db.analytics.domain.auth.internal.InternalCredential;
import com.scalar.db.analytics.domain.authz.BuiltInRole;
import com.scalar.db.analytics.domain.authz.RoleAssignment;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.auth.AuthUserRepository;
import com.scalar.db.analytics.repository.auth.PasswordIdentityRepository;
import com.scalar.db.analytics.repository.auth.internal.InternalCredentialRepository;
import com.scalar.db.analytics.repository.authz.RoleAssignmentRepository;
import com.scalar.db.analytics.repository.authz.RoleRepository;
import com.scalar.db.analytics.repository.impl.spring.repository.authz.AuthzMasterDataSeeder;
import com.scalar.db.analytics.repository.impl.spring.transaction.SpringDataJdbcTransactionContext;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "scalar.db.analytics.server.auth",
    name = "enabled",
    havingValue = "true")
@Order(2)
public class SuperAdminBootstrapRunner implements ApplicationRunner {

  private static final Logger logger = LoggerFactory.getLogger(SuperAdminBootstrapRunner.class);

  private final AuthzMasterDataSeeder masterDataSeeder;
  private final RoleRepository<SpringDataJdbcTransactionContext> roleRepository;
  private final RoleAssignmentRepository<SpringDataJdbcTransactionContext> roleAssignmentRepository;
  private final AuthUserRepository<SpringDataJdbcTransactionContext> authUserRepository;
  private final PasswordIdentityRepository<SpringDataJdbcTransactionContext>
      passwordIdentityRepository;
  private final Optional<InternalCredentialRepository<SpringDataJdbcTransactionContext>>
      internalCredentialRepository;
  private final RepositoryTransactionManager<SpringDataJdbcTransactionContext> txManager;
  private final AuthServerProperties properties;
  private final Optional<PasswordEncoder> passwordEncoder;

  public SuperAdminBootstrapRunner(
      AuthzMasterDataSeeder masterDataSeeder,
      RoleRepository<SpringDataJdbcTransactionContext> roleRepository,
      RoleAssignmentRepository<SpringDataJdbcTransactionContext> roleAssignmentRepository,
      AuthUserRepository<SpringDataJdbcTransactionContext> authUserRepository,
      PasswordIdentityRepository<SpringDataJdbcTransactionContext> passwordIdentityRepository,
      Optional<InternalCredentialRepository<SpringDataJdbcTransactionContext>>
          internalCredentialRepository,
      RepositoryTransactionManager<SpringDataJdbcTransactionContext> txManager,
      AuthServerProperties properties,
      Optional<PasswordEncoder> passwordEncoder) {
    this.masterDataSeeder = masterDataSeeder;
    this.roleRepository = roleRepository;
    this.roleAssignmentRepository = roleAssignmentRepository;
    this.authUserRepository = authUserRepository;
    this.passwordIdentityRepository = passwordIdentityRepository;
    this.internalCredentialRepository = internalCredentialRepository;
    this.txManager = txManager;
    this.properties = properties;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    logger.info("Starting authorization bootstrap");
    masterDataSeeder.seedResourceTypes();
    masterDataSeeder.seedPermissions();
    seedSuperadminRole();
    assignInitialAdmin();
    logger.info("Authorization bootstrap completed");
  }

  private void seedSuperadminRole() throws Exception {
    txManager.withTransaction(
        ctx -> {
          if (roleRepository.findByName(ctx, BuiltInRole.SUPERADMIN_NAME).isEmpty()) {
            roleRepository.create(ctx, BuiltInRole.superadmin());
            logger.info("Created built-in role: {}", BuiltInRole.SUPERADMIN_NAME);
          }
          return null;
        });
  }

  private void assignInitialAdmin() throws Exception {
    String username = properties.getInitialAdminUsername();
    if (username == null || username.isBlank()) {
      return;
    }

    txManager.withTransaction(
        ctx -> {
          List<RoleAssignment> existing =
              roleAssignmentRepository.findByRoleId(ctx, BuiltInRole.SUPERADMIN_ID);
          if (!existing.isEmpty()) {
            logger.info(
                "SUPERADMIN role already has {} assignee(s), skipping initial admin assignment",
                existing.size());
            return null;
          }

          // Bootstrap operates on the INTERNAL backend: this runner only manages the INTERNAL
          // initial-admin credential. JIT-deferred backends assign SUPERADMIN at first login via
          // AuthenticationUseCaseImpl.assignInitialAdminIfApplicable, not here.
          Optional<PasswordIdentity> identity =
              passwordIdentityRepository.findByBackendAndBackendUserId(
                  ctx, PasswordBackendType.INTERNAL, username);
          if (identity.isPresent()) {
            roleAssignmentRepository.create(
                ctx, new RoleAssignment(identity.get().userId(), BuiltInRole.SUPERADMIN_ID));
            logger.info("Assigned user '{}' to SUPERADMIN role", username);
            return null;
          }

          // User does not exist yet — handle based on backend type.
          // We create the user directly via repositories (bypassing UseCase authorization
          // checks) because no SUPERADMIN user exists yet at bootstrap time.
          if (internalCredentialRepository.isPresent() && passwordEncoder.isPresent()) {
            String password = properties.getPassword().getInternal().getInitialAdminPassword();
            if (password == null || password.isBlank()) {
              logger.warn(
                  "Initial admin user '{}' not found and no initial-admin-password configured,"
                      + " skipping SUPERADMIN assignment",
                  username);
              return null;
            }
            AuthUser user = AuthUser.create(username);
            authUserRepository.create(ctx, user);
            PasswordIdentity newIdentity =
                PasswordIdentity.create(user.getUserId(), PasswordBackendType.INTERNAL, username);
            passwordIdentityRepository.create(ctx, newIdentity);
            String passwordHash = passwordEncoder.get().encode(password);
            internalCredentialRepository
                .get()
                .create(ctx, new InternalCredential(username, passwordHash));
            logger.info("Created initial admin user: {}", username);
            roleAssignmentRepository.create(
                ctx, new RoleAssignment(user.getUserId(), BuiltInRole.SUPERADMIN_ID));
            logger.info("Assigned user '{}' to SUPERADMIN role", username);
          } else {
            logger.info(
                "Initial admin '{}' will be assigned SUPERADMIN role on first login", username);
          }
          return null;
        });
  }
}
