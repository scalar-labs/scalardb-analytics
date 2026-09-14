/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.domain.authz;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A reference to a resource for authorization checks. This is a non-persistent value object.
 *
 * @param resourceType the type of the resource
 * @param resourceId the unique identifier of the resource
 * @param dataSourceProviderType the provider type of the owning data source (e.g. "scalardb",
 *     "mysql"). Used by authorization delegates to decide whether backend-specific privilege checks
 *     apply, avoiding redundant data source lookups. Null for resource types that are not
 *     associated with a specific data source (e.g. CATALOG).
 */
public record ResourceRef(
    ResourceType resourceType, UUID resourceId, @Nullable String dataSourceProviderType) {

  /** Creates a ResourceRef without data source provider type information. */
  public ResourceRef(ResourceType resourceType, UUID resourceId) {
    this(resourceType, resourceId, null);
  }
}
