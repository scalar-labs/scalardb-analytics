/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.jdbc;

import java.time.Instant;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;

/**
 * Base entity class that provides common audit fields and Persistable implementation.
 *
 * @param <ID> the type of the entity's identifier
 */
@Getter
@Setter
public abstract class BaseEntity<ID> implements Persistable<ID> {

  @CreatedDate
  @Column("created_at")
  @Nullable
  private Instant createdAt;

  @LastModifiedDate
  @Column("updated_at")
  @Nullable
  private Instant updatedAt;

  @Override
  public boolean isNew() {
    // If createdAt is null, this is a new entity
    return createdAt == null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BaseEntity)) {
      return false;
    }
    BaseEntity<?> that = (BaseEntity<?>) o;
    return Objects.equals(getId(), that.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId());
  }
}
