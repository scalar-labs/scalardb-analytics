/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.jdbc.auth.internal;

import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("auth_internal_credentials")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InternalCredentialEntity extends BaseEntity<String> {

  @Id
  @Column("username")
  private String username;

  @Column("password_hash")
  private String passwordHash;

  @Override
  public String getId() {
    return username;
  }
}
