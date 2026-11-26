/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.compat.spark

import org.apache.spark.sql.jdbc.JdbcDialects
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class JdbcDialectsCompatTest extends AnyFunSuite with Matchers {
  private val databricksUrl = "jdbc:databricks://example.cloud.databricks.com:443"

  test("registerDatabricksDialect makes exactly one dialect handle Databricks URLs") {
    JdbcDialectsCompat.registerDatabricksDialect()

    val dialect = JdbcDialects.get(databricksUrl)

    // Spark returns NoopDialect when no dialect handles the URL and AggregatedDialect when
    // several do. Either would mean the Databricks dialect is missing or registered twice.
    dialect.getClass.getSimpleName should not be "NoopDialect$"
    dialect.getClass.getSimpleName should not be "AggregatedDialect"
    dialect.quoteIdentifier("col") shouldBe "`col`"
  }
}
