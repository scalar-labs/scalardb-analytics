/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.jdbc;

import com.scalar.db.analytics.repository.impl.spring.converter.StringList;
import java.util.List;
import java.util.Objects;
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
@Table("registry_namespaces")
public class NamespaceEntity extends BaseEntity<UUID> {

  @Id
  @Column("namespace_id")
  private UUID namespaceId;

  @Column("data_source_id")
  private UUID dataSourceId;

  @Column("names")
  private StringList names;

  public List<String> getNames() {
    return Objects.requireNonNull(names, "names must not be null").values();
  }

  public void setNames(List<String> names) {
    this.names = new StringList(names);
  }

  public StringList getNamesAsStringList() {
    return names;
  }

  public void setNamesAsStringList(StringList names) {
    this.names = names;
  }

  @Override
  public UUID getId() {
    return namespaceId;
  }
}
