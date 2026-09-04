/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark

import com.scalar.db.analytics.spark.util.{ServerContainer, SparkTestBase}
import org.apache.spark.SparkConf
import org.scalatest.Suite

/** Base trait for catalog integration tests.
  *
  * This trait combines SparkTestBase and ServerContainer to provide:
  *   - SparkSession lifecycle management
  *   - Server container lifecycle management
  *   - Common test utilities
  *   - Standard Spark catalog configuration
  *
  * Concrete test classes should:
  *   1. Define CATALOG_NAME constant 2. Start database containers (MySQL, PostgreSQL, etc.) 3.
  *      Override serverConfig to provide database connection settings 4. Start the server container
  *      with startServerBundle() 5. Configure Spark catalog using the provided config method
  */
trait CatalogTestBase extends SparkTestBase with ServerContainer {
  this: Suite =>

  /** Catalog name to use in tests. Must be defined by concrete test classes.
    */
  protected def CATALOG_NAME: String

  /** Catalog prefix for Spark configuration.
    */
  protected lazy val CATALOG_PREFIX: String = s"spark.sql.catalog.$CATALOG_NAME"

  /** Default Spark configuration for catalog integration tests.
    *
    * Configures:
    *   - ScalarDB Analytics Catalog implementation
    *   - Server endpoint (host and catalog port)
    */
  override protected def config: SparkConf = {
    val conf = new SparkConf()
    conf.set(CATALOG_PREFIX, "com.scalar.db.analytics.spark.ScalarDbAnalyticsCatalog")
    conf.set(s"$CATALOG_PREFIX.server.host", "localhost")
    conf.set(s"$CATALOG_PREFIX.server.catalog.port", serverEndpoint.split(":")(1))
    conf
  }
}
