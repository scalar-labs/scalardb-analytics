/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.scalardb.table

import com.scalar.db.analytics.spark.lib.catalog.{CatalogIdentifier, CatalogNamespace}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

@SuppressWarnings(Array("org.wartremover.warts.Null"))
class ScalarDbScanTest extends AnyFunSuite with Matchers {

  private val identifier1 =
    CatalogIdentifier(CatalogNamespace("ds", List("ns")), "tbl")
  private val identifier2 =
    CatalogIdentifier(CatalogNamespace("ds", List("ns")), "tbl")
  private val differentIdentifier =
    CatalogIdentifier(CatalogNamespace("ds", List("ns")), "other")

  private def createScan(id: CatalogIdentifier): ScalarDbScan =
    new ScalarDbScan(id, null, null, None, None, None)

  test("equals should return true for scans with the same identifier") {
    val scan1 = createScan(identifier1)
    val scan2 = createScan(identifier2)
    scan1 shouldEqual scan2
  }

  test("hashCode should be the same for scans with the same identifier") {
    val scan1 = createScan(identifier1)
    val scan2 = createScan(identifier2)
    scan1.hashCode() shouldEqual scan2.hashCode()
  }

  test("equals should return false for scans with different identifiers") {
    val scan1 = createScan(identifier1)
    val scan2 = createScan(differentIdentifier)
    scan1 should not equal scan2
  }

  test("equals should return false for null") {
    val scan = createScan(identifier1)
    scan should not equal null
  }

  test("equals should return false for a different type") {
    val scan = createScan(identifier1)
    scan should not equal "not a scan"
  }
}
