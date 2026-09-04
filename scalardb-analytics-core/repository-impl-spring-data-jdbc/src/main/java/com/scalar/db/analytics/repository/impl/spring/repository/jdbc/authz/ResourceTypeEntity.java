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

@Table("authz_resource_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ResourceTypeEntity extends BaseEntity<UUID> {

  @Id
  @Column("id")
  private UUID id;

  @Column("name")
  private String name;

  @Override
  public UUID getId() {
    return id;
  }
}
