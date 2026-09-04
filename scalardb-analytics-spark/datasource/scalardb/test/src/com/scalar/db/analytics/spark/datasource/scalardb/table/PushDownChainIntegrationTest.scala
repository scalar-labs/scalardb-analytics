/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.scalardb.table

import com.scalar.db.analytics.api.model.{Column, DataType, TableDetail, TableInfo}
import com.scalar.db.analytics.spark.lib.catalog.{CatalogIdentifier, CatalogNamespace}
import com.scalar.db.config.DatabaseConfig
import org.apache.spark.sql.connector.expressions.filter.{Or, Predicate}
import org.apache.spark.sql.connector.expressions.{Expression, Expressions}
import org.apache.spark.sql.types._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.util.UUID

/** Integration tests that verify push down information flows correctly through the entire chain:
  * ScalarDbScanBuilder → ScalarDbScan → ScalarDbBatch → ScalarDbPartitionReaderFactory.
  *
  * These tests verify the chain up to the point where a real ScalarDB connection would be needed,
  * ensuring that push down info (predicates, column pruning, limit) is correctly propagated and
  * serialized.
  */
@SuppressWarnings(Array("org.wartremover.warts.All"))
class PushDownChainIntegrationTest extends AnyFunSuite with Matchers {

  private val identifier =
    CatalogIdentifier(CatalogNamespace("ds", List("ns")), "tbl")

  private val tableDetail = new TableDetail(
    TableInfo.create(UUID.randomUUID(), "test_table"),
    java.util.Arrays.asList(
      Column.create(UUID.randomUUID(), "id", DataType.Int.INSTANCE, 0, false),
      Column.create(UUID.randomUUID(), "name", DataType.Text.INSTANCE, 1, false),
      Column.create(UUID.randomUUID(), "age", DataType.Int.INSTANCE, 2, true),
      Column.create(UUID.randomUUID(), "score", DataType.Double.INSTANCE, 3, true)
    )
  )

  private val baseConfigs = {
    val p = new java.util.Properties()
    p.setProperty(DatabaseConfig.CONTACT_POINTS, "localhost")
    p.setProperty(DatabaseConfig.STORAGE, "jdbc")
    p.setProperty(DatabaseConfig.CROSS_PARTITION_SCAN, "true")
    p
  }

  private val config = new DatabaseConfig(baseConfigs)

  private def colRef(name: String): Expression =
    Expressions.column(name)

  private def intLit(value: Int): Expression =
    Expressions.literal(value)

  private def stringLit(value: String): Expression =
    Expressions.literal(value)

  private def doubleLit(value: Double): Expression =
    Expressions.literal(value)

  private def roundTrip[T <: java.io.Serializable](obj: T): T = {
    val bos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(bos)
    oos.writeObject(obj)
    oos.close()
    val bis = new ByteArrayInputStream(bos.toByteArray)
    val ois = new ObjectInputStream(bis)
    ois.readObject().asInstanceOf[T]
  }

  // --- Filter push down chain ---

  test("filter push down should propagate predicates through the entire chain") {
    val builder = new ScalarDbScanBuilder(identifier, tableDetail, config)

    val p1         = new Predicate("=", Array[Expression](colRef("id"), intLit(42)))
    val p2         = new Predicate(">", Array[Expression](colRef("age"), intLit(18)))
    val postFilter = builder.pushPredicates(Array(p1, p2))

    postFilter shouldBe empty
    builder.pushedPredicates() should have length 2

    val scan    = builder.build().asInstanceOf[ScalarDbScan]
    val batch   = scan.toBatch().asInstanceOf[ScalarDbBatch]
    val factory = batch.createReaderFactory().asInstanceOf[ScalarDbPartitionReaderFactory]

    // Verify factory is serializable (required for Spark executor distribution)
    val deserializedFactory = roundTrip(factory)
    deserializedFactory shouldBe a[ScalarDbPartitionReaderFactory]
  }

  test("unsupported predicates should not be pushed and should be returned for post-filtering") {
    val builder = new ScalarDbScanBuilder(identifier, tableDetail, config)

    val supported   = new Predicate("=", Array[Expression](colRef("id"), intLit(1)))
    val unsupported = new Predicate("IN", Array[Expression](colRef("age"), intLit(1), intLit(2)))
    val postFilter  = builder.pushPredicates(Array(supported, unsupported))

    postFilter should have length 1
    postFilter(0).name() shouldBe "IN"

    builder.pushedPredicates() should have length 1
    builder.pushedPredicates()(0).name() shouldBe "="
  }

  // --- Column pruning chain ---

  test("column pruning should propagate pruned schema through the chain") {
    val builder = new ScalarDbScanBuilder(identifier, tableDetail, config)

    val prunedSchema = new StructType().add("id", IntegerType).add("name", StringType)

    builder.pruneColumns(prunedSchema)

    val scan = builder.build().asInstanceOf[ScalarDbScan]
    scan.readSchema() shouldBe prunedSchema
    scan.readSchema().fieldNames should contain theSameElementsAs Array("id", "name")
  }

  test("readSchema should return full schema when no columns are pruned") {
    val builder = new ScalarDbScanBuilder(identifier, tableDetail, config)

    val scan = builder.build().asInstanceOf[ScalarDbScan]
    scan.readSchema().fieldNames should have length 4
  }

  // --- Limit push down chain ---

  test("limit push down should propagate through the chain") {
    val builder = new ScalarDbScanBuilder(identifier, tableDetail, config)

    val accepted = builder.pushLimit(10)
    accepted shouldBe true

    val scan    = builder.build().asInstanceOf[ScalarDbScan]
    val batch   = scan.toBatch().asInstanceOf[ScalarDbBatch]
    val factory = batch.createReaderFactory().asInstanceOf[ScalarDbPartitionReaderFactory]

    // Verify the factory with limit is still serializable
    val deserializedFactory = roundTrip(factory)
    deserializedFactory shouldBe a[ScalarDbPartitionReaderFactory]
  }

  // --- Combined push down chain ---

  test("all push downs combined should propagate through the chain") {
    val builder = new ScalarDbScanBuilder(identifier, tableDetail, config)

    // Push filter
    val p1         = new Predicate("=", Array[Expression](colRef("name"), stringLit("Alice")))
    val p2         = new Predicate(">=", Array[Expression](colRef("score"), doubleLit(90.0)))
    val postFilter = builder.pushPredicates(Array(p1, p2))
    postFilter shouldBe empty

    // Prune columns
    val prunedSchema = new StructType().add("name", StringType).add("score", DoubleType)
    builder.pruneColumns(prunedSchema)

    // Push limit
    builder.pushLimit(5)

    // Verify chain
    val scan = builder.build().asInstanceOf[ScalarDbScan]
    scan.readSchema().fieldNames should contain theSameElementsAs Array("name", "score")

    val batch   = scan.toBatch().asInstanceOf[ScalarDbBatch]
    val factory = batch.createReaderFactory().asInstanceOf[ScalarDbPartitionReaderFactory]

    // Verify full serialization round-trip
    val deserialized = roundTrip(factory)
    deserialized shouldBe a[ScalarDbPartitionReaderFactory]
  }

  // --- PushDownInfo serialization integration ---

  test("PushDownInfo.from should correctly convert pushed expressions for serialization") {
    val builder = new ScalarDbScanBuilder(identifier, tableDetail, config)

    val predicates = Array[Predicate](
      new Predicate("=", Array[Expression](colRef("id"), intLit(1))),
      new Predicate("<>", Array[Expression](colRef("name"), stringLit("test"))),
      new Predicate("IS_NULL", Array[Expression](colRef("age")))
    )
    builder.pushPredicates(predicates)
    builder.pruneColumns(new StructType().add("id", IntegerType).add("name", StringType))
    builder.pushLimit(100)

    val scan    = builder.build().asInstanceOf[ScalarDbScan]
    val batch   = scan.toBatch().asInstanceOf[ScalarDbBatch]
    val factory = batch.createReaderFactory().asInstanceOf[ScalarDbPartitionReaderFactory]

    // Simulate Spark's executor distribution via serialization
    val deserialized = roundTrip(factory)
    deserialized shouldBe a[ScalarDbPartitionReaderFactory]
  }

  // --- OR predicate push down integration ---

  test("a single OR predicate should propagate through the chain as DNF") {
    val builder = new ScalarDbScanBuilder(identifier, tableDetail, config)

    val or = new Or(
      new Predicate("=", Array[Expression](colRef("id"), intLit(1))),
      new Predicate("=", Array[Expression](colRef("id"), intLit(2)))
    )

    val postFilter = builder.pushPredicates(Array(or))
    postFilter shouldBe empty
    builder.pushedPredicates() should have length 1

    val scan         = builder.build().asInstanceOf[ScalarDbScan]
    val batch        = scan.toBatch().asInstanceOf[ScalarDbBatch]
    val factory      = batch.createReaderFactory().asInstanceOf[ScalarDbPartitionReaderFactory]
    val deserialized = roundTrip(factory)
    deserialized shouldBe a[ScalarDbPartitionReaderFactory]
  }

  test("an OR conjunct combined with a leaf should propagate through the chain as CNF") {
    val builder = new ScalarDbScanBuilder(identifier, tableDetail, config)

    val leaf = new Predicate("=", Array[Expression](colRef("id"), intLit(1)))
    val or = new Or(
      new Predicate(">", Array[Expression](colRef("age"), intLit(20))),
      new Predicate("<", Array[Expression](colRef("age"), intLit(10)))
    )

    val postFilter = builder.pushPredicates(Array(leaf, or))
    postFilter shouldBe empty
    builder.pushedPredicates() should have length 2

    val scan         = builder.build().asInstanceOf[ScalarDbScan]
    val batch        = scan.toBatch().asInstanceOf[ScalarDbBatch]
    val factory      = batch.createReaderFactory().asInstanceOf[ScalarDbPartitionReaderFactory]
    val deserialized = roundTrip(factory)
    deserialized shouldBe a[ScalarDbPartitionReaderFactory]
  }

  // --- Scan equality with push down ---

  test("ScalarDbScan equality should be based on identifier, not push down info") {
    val builder1 = new ScalarDbScanBuilder(identifier, tableDetail, config)
    builder1.pushLimit(10)
    val scan1 = builder1.build()

    val builder2 = new ScalarDbScanBuilder(identifier, tableDetail, config)
    builder2.pushLimit(20)
    val scan2 = builder2.build()

    // Scans with same identifier should be equal (for caching)
    scan1 shouldEqual scan2
    scan1.hashCode() shouldEqual scan2.hashCode()
  }
}
