/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.compat.spark

object JdbcDialectsCompat {

  /** Does nothing: Spark 4 registers its own Databricks dialect by default.
    *
    * Registering the Spark 3.5 backport as well would make Spark combine both dialects into an
    * `AggregatedDialect` for `jdbc:databricks` URLs, in which the backport takes precedence and
    * hides the improvements of the built-in dialect, such as escaping backticks in identifiers.
    */
  def registerDatabricksDialect(): Unit = ()
}
