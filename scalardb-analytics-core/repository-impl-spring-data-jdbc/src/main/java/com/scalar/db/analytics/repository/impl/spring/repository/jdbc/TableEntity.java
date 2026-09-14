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

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("registry_tables")
public class TableEntity extends BaseEntity<UUID> {

  @Id
  @Column("table_id")
  private UUID tableId;

  @Column("namespace_id")
  private UUID namespaceId;

  @Column("name")
  private String name;

  @Override
  public UUID getId() {
    return tableId;
  }
}
