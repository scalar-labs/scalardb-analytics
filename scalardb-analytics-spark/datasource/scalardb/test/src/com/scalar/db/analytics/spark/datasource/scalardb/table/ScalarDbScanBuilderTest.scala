/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.scalardb.table

import com.scalar.db.analytics.spark.lib.catalog.{CatalogIdentifier, CatalogNamespace}
import org.apache.spark.sql.connector.expressions.filter.{And, Or, Predicate}
import org.apache.spark.sql.connector.expressions.{Expression, Expressions}
import org.apache.spark.sql.types._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

@SuppressWarnings(Array("org.wartremover.warts.All"))
class ScalarDbScanBuilderTest extends AnyFunSuite with Matchers {

  private val identifier =
    CatalogIdentifier(CatalogNamespace("ds", List("ns")), "tbl")

  private def createBuilder(): ScalarDbScanBuilder =
    new ScalarDbScanBuilder(identifier, null, null)

  private def colRef(name: String): Expression =
    Expressions.column(name)

  private def intLit(value: Int): Expression =
    Expressions.literal(value)

  private def cmp(op: String, left: Expression, right: Expression): Predicate =
    new Predicate(op, Array[Expression](left, right))

  // --- pushPredicates ---

  test("pushPredicates should accept supported predicates and return unsupported ones") {
    val builder     = createBuilder()
    val supported   = cmp("=", colRef("a"), intLit(1))
    val unsupported = new Predicate("IN", Array[Expression](colRef("b"), intLit(1), intLit(2)))

    val postFilter = builder.pushPredicates(Array(supported, unsupported))

    postFilter should have length 1
    postFilter(0).name() shouldBe "IN"

    builder.pushedPredicates() should have length 1
    builder.pushedPredicates()(0).name() shouldBe "="
  }

  test("pushPredicates should accept all predicates when all are supported") {
    val builder = createBuilder()
    val p1      = cmp("=", colRef("a"), intLit(1))
    val p2      = cmp(">", colRef("b"), intLit(2))

    val postFilter = builder.pushPredicates(Array(p1, p2))

    postFilter shouldBe empty
    builder.pushedPredicates() should have length 2
  }

  test("pushPredicates should return all predicates when none are supported") {
    val builder = createBuilder()
    val p1      = new Predicate("IN", Array[Expression](colRef("a"), intLit(1)))
    val p2      = new Predicate("LIKE", Array[Expression](colRef("b"), intLit(2)))

    val postFilter = builder.pushPredicates(Array(p1, p2))

    postFilter should have length 2
    builder.pushedPredicates() shouldBe empty
  }

  test("pushPredicates should push a flat OR conjunct") {
    val builder = createBuilder()
    val or      = new Or(cmp("=", colRef("a"), intLit(1)), cmp("=", colRef("b"), intLit(2)))

    val postFilter = builder.pushPredicates(Array(or))

    postFilter shouldBe empty
    builder.pushedPredicates() should have length 1
  }

  test("pushPredicates should push a single OR predicate as DNF") {
    val builder = createBuilder()
    val and1    = new And(cmp("=", colRef("a"), intLit(1)), cmp("=", colRef("b"), intLit(2)))
    val and2    = new And(cmp("=", colRef("c"), intLit(3)), cmp("=", colRef("d"), intLit(4)))
    val or      = new Or(and1, and2)

    val postFilter = builder.pushPredicates(Array(or))

    postFilter shouldBe empty
    builder.pushedPredicates() should have length 1
  }

  test("pushPredicates should defer an OR conjunct that contains a nested AND") {
    val builder = createBuilder()
    val leaf    = cmp("=", colRef("d"), intLit(4))
    val dnfShaped = new Or(
      new And(cmp("=", colRef("a"), intLit(1)), cmp("=", colRef("b"), intLit(2))),
      cmp("=", colRef("c"), intLit(3))
    )

    val postFilter = builder.pushPredicates(Array(leaf, dnfShaped))

    postFilter should have length 1
    builder.pushedPredicates() should have length 1
    builder.pushedPredicates()(0) shouldBe leaf
  }

  // --- pruneColumns ---

  test("pruneColumns should store the required schema") {
    val builder = createBuilder()
    val schema  = new StructType().add("col1", IntegerType).add("col2", StringType)

    builder.pruneColumns(schema)

    val scan = builder.build().asInstanceOf[ScalarDbScan]
    scan.readSchema() shouldBe schema
  }

  // --- pushLimit ---

  test("pushLimit should return true and store the limit") {
    val builder = createBuilder()

    val result = builder.pushLimit(100)

    result shouldBe true
  }

  // --- Combined push down ---

  test("build should produce ScalarDbScan with push down info") {
    val builder = createBuilder()

    val pred = new Predicate("=", Array[Expression](colRef("a"), intLit(1)))
    builder.pushPredicates(Array(pred))

    val schema = new StructType().add("a", IntegerType)
    builder.pruneColumns(schema)

    builder.pushLimit(50)

    val scan = builder.build()
    scan shouldBe a[ScalarDbScan]
    scan.readSchema() shouldBe schema
  }
}
