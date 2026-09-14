/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.jdbc.auth;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.BaseEntity;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("auth_password_identities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PasswordIdentityEntity extends BaseEntity<UUID> {

  @Id
  @Column("identity_id")
  private UUID identityId;

  @Column("user_id")
  private UUID userId;

  @Column("backend")
  private PasswordBackendType backend;

  @Column("backend_user_id")
  private String backendUserId;

  @Override
  public UUID getId() {
    return identityId;
  }
}
