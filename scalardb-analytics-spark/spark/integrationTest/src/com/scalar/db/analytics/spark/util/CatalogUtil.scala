/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.util

import com.scalar.db.analytics.spark.ScalarDbAnalyticsCatalog
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.connector.catalog.CatalogPlugin

object CatalogUtil {
  def getCatalog(session: SparkSession, name: String): ScalarDbAnalyticsCatalog = {
    val catalog: CatalogPlugin = session.sessionState.catalogManager.catalog(name)

    if (catalog == null) {
      throw new IllegalArgumentException(s"Catalog not found: $name")
    }

    catalog match {
      case scalarDbCatalog: ScalarDbAnalyticsCatalog => scalarDbCatalog
      case _ =>
        throw new IllegalArgumentException(
          s"Catalog `$name` is not an instance of ScalarDbAnalyticsCatalog: ${catalog.getClass.getName}"
        )
    }
  }
}
