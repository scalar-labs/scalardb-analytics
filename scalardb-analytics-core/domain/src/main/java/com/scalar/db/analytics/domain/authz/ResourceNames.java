/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.domain.authz;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The names that identify a resource, from its catalog down to the resource itself. Names below the
 * level of the resource are null (or empty for the namespace names): a catalog has only a catalog
 * name, and a table has all four.
 *
 * @param catalogName the name of the catalog the resource belongs to, or the catalog itself
 * @param dataSourceName the name of the data source, or null for a catalog
 * @param namespaceNames the namespace names, or empty for a catalog or a data source
 * @param tableName the name of the table, or null for a resource above the table level
 */
public record ResourceNames(
    String catalogName,
    @Nullable String dataSourceName,
    List<String> namespaceNames,
    @Nullable String tableName) {

  public ResourceNames {
    namespaceNames = List.copyOf(namespaceNames);
  }
}
