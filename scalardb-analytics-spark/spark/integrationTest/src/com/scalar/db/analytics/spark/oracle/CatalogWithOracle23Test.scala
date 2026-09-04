/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.oracle

import com.scalar.db.analytics.spark.util.TestImages
import org.testcontainers.utility.DockerImageName

class CatalogWithOracle23Test extends CatalogWithOracleTestBase {
  override protected def oracleImage: DockerImageName = TestImages.ORACLE_23
  override protected def initScript: String           = "sql/init_oracle.sql"
  override protected def oracleServiceName: String    = "FREEPDB1"
}
