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
 * Entity for the role_assignments table with composite key (user_id, role_id).
 *
 * <p>The ScalarDB partition key is user_id and the clustering key is role_id. The {@link Id}
 * annotation marks only the partition key (user_id) due to Spring Data JDBC + ScalarDbRepository
 * constraints. Equality uses both user_id and role_id via {@link EqualsAndHashCode}.
 */
@Table("authz_role_assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RoleAssignmentEntity extends BaseEntity<UUID> {

  @Id
  @Column("user_id")
  private UUID userId;

  @Column("role_id")
  private UUID roleId;

  @Override
  public UUID getId() {
    return userId;
  }
}
