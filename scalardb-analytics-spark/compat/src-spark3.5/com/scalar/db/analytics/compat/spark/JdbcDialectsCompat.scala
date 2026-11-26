/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.compat.spark

import org.apache.spark.sql.jdbc.JdbcDialects

object JdbcDialectsCompat {

  /** Registers the backported [[DatabricksDialect]], which Spark 3.5 does not provide. */
  def registerDatabricksDialect(): Unit =
    JdbcDialects.registerDialect(DatabricksDialect)
}
