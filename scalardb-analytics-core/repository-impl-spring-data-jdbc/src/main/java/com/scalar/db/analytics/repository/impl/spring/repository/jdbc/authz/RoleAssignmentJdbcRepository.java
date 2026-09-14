/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.jdbc.authz;

import com.scalar.db.sql.springdata.ScalarDbRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleAssignmentJdbcRepository
    extends ScalarDbRepository<RoleAssignmentEntity, UUID> {
  List<RoleAssignmentEntity> findByUserId(UUID userId);

  @Query(
      "SELECT user_id, role_id, created_at, updated_at "
          + "FROM scalardb_analytics.authz_role_assignments "
          + "WHERE user_id = :userId AND role_id = :roleId")
  Optional<RoleAssignmentEntity> findByUserIdAndRoleId(
      @Param("userId") UUID userId, @Param("roleId") UUID roleId);

  void deleteByUserId(UUID userId);

  @Modifying
  @Query(
      "DELETE FROM scalardb_analytics.authz_role_assignments "
          + "WHERE user_id = :userId AND role_id = :roleId")
  void deleteByUserIdAndRoleId(@Param("userId") UUID userId, @Param("roleId") UUID roleId);

  @Modifying
  @Query("DELETE FROM scalardb_analytics.authz_role_assignments WHERE role_id = :roleId")
  void deleteByRoleId(@Param("roleId") UUID roleId);

  @Query(
      "SELECT user_id, role_id, created_at, updated_at "
          + "FROM scalardb_analytics.authz_role_assignments WHERE role_id = :roleId")
  List<RoleAssignmentEntity> findByRoleId(@Param("roleId") UUID roleId);
}
