/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.jdbc.auth;

import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.BaseEntity;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("auth_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserEntity extends BaseEntity<UUID> {

  @Id
  @Column("user_id")
  private UUID userId;

  @Column("username")
  private String username;

  @Override
  public UUID getId() {
    return userId;
  }
}
