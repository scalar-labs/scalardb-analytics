/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.model;

import java.util.UUID;
import lombok.Value;

/**
 * Represents a tenant in a multi-tenant system.
 *
 * <p>This class is introduced to prepare for future multi-tenancy support while solving the
 * immediate problem of avoiding full table scans on the catalogs table.
 *
 * <p><strong>Why is tenant_id necessary for catalogs?</strong>
 *
 * <p>In ScalarDB, queries without a partition key or secondary index in the WHERE clause require a
 * full table scan, which internally becomes a cross-partition scan across all partitions. The
 * original catalog.list() query had no WHERE clause, causing it to fail when
 * cross_partition_scan.enabled=false (our target configuration for production).
 *
 * <p>By introducing tenant_id as a secondary index key, catalog queries can use WHERE tenant_id = ?
 * instead of scanning all partitions, eliminating the need for cross-partition scan capability.
 *
 * <p><strong>Why use a fixed default tenant ID?</strong>
 *
 * <p>Currently, the system operates in single-tenant mode. Using a fixed UUID
 * (00000000-0000-0000-0000-000000000000) as the default tenant ID allows us to:
 *
 * <ul>
 *   <li>Avoid cross-partition scans by providing a secondary index WHERE clause
 *   <li>Maintain data consistency - all catalogs belong to the same logical tenant
 *   <li>Enable seamless migration to multi-tenancy in the future without data migration
 * </ul>
 *
 * <p><strong>Future multi-tenancy implementation plan:</strong>
 *
 * <ol>
 *   <li>Create a tenants table with tenant metadata (name, settings, etc.)
 *   <li>Insert a record with id=DEFAULT_ID representing the default tenant
 *   <li>Expand this Tenant class with additional fields (name, createdAt, etc.)
 *   <li>Implement TenantRepository for tenant CRUD operations
 *   <li>Integrate with authentication/authorization to isolate tenant data
 *   <li>Add tenant_id foreign key constraints (if ScalarDB supports them in the future)
 * </ol>
 *
 * <p><strong>Note on ScalarDB constraints:</strong>
 *
 * <p>ScalarDB does not currently support foreign key constraints, so referential integrity between
 * catalogs.tenant_id and a future tenants table must be maintained at the application layer.
 */
@Value
public class Tenant {
  /**
   * Default tenant ID used in single-tenant mode.
   *
   * <p>This UUID (00000000-0000-0000-0000-000000000000) is reserved for the default tenant. When
   * implementing multi-tenancy in the future, create a tenants table record with this ID to
   * represent the default tenant.
   *
   * <p>All existing catalogs use this tenant ID, so no data migration will be required when adding
   * the tenants table.
   */
  public static final UUID DEFAULT_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  UUID id;
}
