/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.jdbc;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("catalogs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CatalogEntity extends BaseEntity<UUID> {

  @Id
  @Column("catalog_id")
  private UUID catalogId;

  @Column("tenant_id")
  private UUID tenantId;

  @Column("name")
  private String name;

  @Override
  public UUID getId() {
    return catalogId;
  }
}
