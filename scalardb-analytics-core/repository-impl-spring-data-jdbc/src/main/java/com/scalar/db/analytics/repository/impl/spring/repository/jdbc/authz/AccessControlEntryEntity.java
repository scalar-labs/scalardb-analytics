/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.jdbc.authz;

import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.BaseEntity;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Entity for the access_control_entries table with composite key (grantee_id, permission_id,
 * resource_id, grantee_type).
 *
 * <p>The ScalarDB partition key is grantee_id and the clustering keys are (permission_id,
 * resource_id, grantee_type). The {@link Id} annotation marks only the partition key (grantee_id)
 * due to Spring Data JDBC + ScalarDbRepository constraints. Equality uses all fields via {@link
 * EqualsAndHashCode}.
 */
@Table("authz_access_control_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AccessControlEntryEntity extends BaseEntity<UUID> {

  @Column("grantee_type")
  private String granteeType;

  @Id
  @Column("grantee_id")
  private UUID granteeId;

  @Column("resource_id")
  private UUID resourceId;

  @Column("permission_id")
  private UUID permissionId;

  @Override
  public UUID getId() {
    return granteeId;
  }
}
