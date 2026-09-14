/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.jdbc.auth;

import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.BaseEntity;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("auth_access_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AccessTokenEntity extends BaseEntity<UUID> {

  @Column("token")
  private String token;

  @Id
  @Column("user_id")
  private UUID userId;

  @Column("expires_at")
  private Instant expiresAt;

  @Override
  public UUID getId() {
    return userId;
  }
}
