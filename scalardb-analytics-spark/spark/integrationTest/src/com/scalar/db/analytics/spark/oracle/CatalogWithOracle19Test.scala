/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.oracle

import com.scalar.db.analytics.spark.util.TestImages
import org.testcontainers.utility.DockerImageName

class CatalogWithOracle19Test extends CatalogWithOracleTestBase {
  override protected def oracleImage: DockerImageName = TestImages.ORACLE_19
  override protected def initScript: String           = "sql/init_oracle_19.sql"
  override protected def oracleServiceName: String    = "ORCLPDB1"

  // Oracle 19c does not support BOOLEAN type in SQL
  override protected def hasBooleanColumn: Boolean = false
}
