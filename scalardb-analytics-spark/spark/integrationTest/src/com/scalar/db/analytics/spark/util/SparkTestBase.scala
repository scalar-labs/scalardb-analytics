/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.util

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

import java.nio.file.{Files, Path}

/** Base trait for tests that require a SparkSession. This provides lifecycle management for
  * SparkSession and warehouse directory.
  */
trait SparkTestBase extends BeforeAndAfterAll with BeforeAndAfterEach {
  this: Suite =>
  private var _spark: Option[SparkSession] = None
  private var warehouseDir: Option[Path]   = None

  /** Returns the Spark session. It is the caller's responsibility to initialize the Spark session
    * before calling this method. If the Spark session is not initialized, it throws an exception.
    *
    * @return
    *   the Spark session
    */
  protected def spark: SparkSession = _spark.getOrElse(
    throw new IllegalStateException("SparkSession is not initialized")
  )

  /** Returns the Spark configuration. Each test can override this method to set the configuration.
    *
    * @return
    *   the Spark configuration for this test class.
    */
  protected def config: SparkConf = new SparkConf()

  /** Indicates whether the Spark session is created per test or not. If this value equals true, the
    * Spark session is created for each test. Otherwise, the Spark session is created once for all
    * tests.
    *
    * @return
    *   true if the Spark session is created for each test.
    */
  protected def isSessionPerTest: Boolean = false

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    if (isSessionPerTest && _spark.isEmpty) {
      _spark = Some(
        SparkSession.builder().master("local").config(configWithDefault()).getOrCreate()
      )
    }
  }

  override protected def afterEach(): Unit = {
    if (isSessionPerTest && _spark.isDefined) {
      _spark.foreach(_.stop())
      _spark = None
    }
    super.afterEach()
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    // Create a temporary warehouse directory
    warehouseDir = Some(Files.createTempDirectory("spark-warehouse-"))

    if (!isSessionPerTest && _spark.isEmpty) {
      _spark = Some(
        SparkSession.builder().master("local").config(configWithDefault()).getOrCreate()
      )
    }
  }

  override protected def afterAll(): Unit = {
    if (!isSessionPerTest && _spark.isDefined) {
      _spark.foreach(_.stop())
      _spark = None
    }

    // Clean up warehouse directory
    warehouseDir.foreach { dir =>
      deleteRecursively(dir)
    }

    super.afterAll()
  }

  private def configWithDefault(): SparkConf = {
    val conf = config
    warehouseDir.foreach { dir =>
      conf.set("spark.sql.warehouse.dir", dir.toString)
    }
    conf
  }

  private def deleteRecursively(path: Path): Unit = {
    import java.util.Comparator
    if (Files.exists(path)) {
      Files.walk(path).sorted(Comparator.reverseOrder()).forEach(Files.delete(_))
    }
  }
}
