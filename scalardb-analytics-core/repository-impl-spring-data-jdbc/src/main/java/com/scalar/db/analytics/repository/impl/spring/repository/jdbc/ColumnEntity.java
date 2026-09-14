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
@Table("registry_columns")
public class ColumnEntity extends BaseEntity<UUID> {

  @Id
  @Column("column_id")
  private UUID columnId;

  @Column("table_id")
  private UUID tableId;

  @Column("name")
  private String name;

  @Column("type")
  private String type;

  @Column("ordinal_position")
  private int ordinalPosition;

  @Column("nullable")
  private boolean nullable;

  @Override
  public UUID getId() {
    return columnId;
  }
}
