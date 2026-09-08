/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Catalog {
  // tenantId is an internal field always set to Tenant.DEFAULT_ID.
  // Multi-tenancy is not yet supported, so this value is fixed.
  @JsonIgnore UUID tenantId;
  UUID id;
  String name;

  /** Creates a new catalog with an auto-generated ID. */
  public static Catalog create(String name) {
    return new Catalog(Tenant.DEFAULT_ID, UUID.randomUUID(), name);
  }

  /** Reconstructs a catalog from persisted data. */
  public static Catalog of(UUID id, String name) {
    return new Catalog(Tenant.DEFAULT_ID, id, name);
  }
}
