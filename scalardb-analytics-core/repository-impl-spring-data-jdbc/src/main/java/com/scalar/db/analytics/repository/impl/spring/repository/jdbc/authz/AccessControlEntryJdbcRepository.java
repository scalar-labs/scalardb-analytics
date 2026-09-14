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
public interface AccessControlEntryJdbcRepository
    extends ScalarDbRepository<AccessControlEntryEntity, UUID> {

  @Query(
      "SELECT grantee_type, grantee_id, resource_id, permission_id, created_at, updated_at "
          + "FROM scalardb_analytics.authz_access_control_entries "
          + "WHERE grantee_id = :granteeId")
  List<AccessControlEntryEntity> findByGranteeId(@Param("granteeId") UUID granteeId);

  @Query(
      "SELECT grantee_type, grantee_id, resource_id, permission_id, created_at, updated_at "
          + "FROM scalardb_analytics.authz_access_control_entries "
          + "WHERE grantee_id = :granteeId "
          + "AND permission_id = :permissionId AND resource_id = :resourceId")
  Optional<AccessControlEntryEntity> findByGranteeAndPermissionAndResource(
      @Param("granteeId") UUID granteeId,
      @Param("permissionId") UUID permissionId,
      @Param("resourceId") UUID resourceId);

  @Modifying
  @Query(
      "DELETE FROM scalardb_analytics.authz_access_control_entries "
          + "WHERE grantee_type = :granteeType AND grantee_id = :granteeId "
          + "AND permission_id = :permissionId AND resource_id = :resourceId")
  void deleteByGranteeAndPermissionAndResource(
      @Param("granteeType") String granteeType,
      @Param("granteeId") UUID granteeId,
      @Param("permissionId") UUID permissionId,
      @Param("resourceId") UUID resourceId);

  @Modifying
  @Query(
      "DELETE FROM scalardb_analytics.authz_access_control_entries WHERE grantee_id = :granteeId")
  void deleteByGranteeId(@Param("granteeId") UUID granteeId);

  @Modifying
  @Query(
      "DELETE FROM scalardb_analytics.authz_access_control_entries WHERE resource_id = :resourceId")
  void deleteByResourceId(@Param("resourceId") UUID resourceId);
}
