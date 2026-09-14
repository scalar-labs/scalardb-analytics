/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.scalardb.table

import com.scalar.db.analytics.spark.lib.catalog.CatalogIdentifier

object NamespaceUtil {
  def getScalarDbNamespace(identifier: CatalogIdentifier): String =
    // The namespace of the ScalarDB-backed table is always a single level.
    identifier.namespace.namespace.apply(0)
}
