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

@Table("registry_data_sources")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DataSourceEntity extends BaseEntity<UUID> {

  @Id
  @Column("data_source_id")
  private UUID dataSourceId;

  @Column("catalog_id")
  private UUID catalogId;

  @Column("name")
  private String name;

  @Column("provider_type")
  private String providerType;

  @Column("provider_payload_json")
  private String providerPayloadJson;

  @Override
  public UUID getId() {
    return dataSourceId;
  }
}
