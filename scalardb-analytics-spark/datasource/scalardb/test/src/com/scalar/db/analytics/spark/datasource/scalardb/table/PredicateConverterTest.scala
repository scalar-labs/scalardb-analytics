/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.scalardb.table

import com.scalar.db.api.ConditionalExpression
import com.scalar.db.api.ConditionalExpression.Operator
import com.scalar.db.io.{DataType => ScalarDbDataType}
import org.apache.spark.sql.connector.expressions.filter.{And, Or, Predicate}
import org.apache.spark.sql.connector.expressions.{Expression, Expressions, Literal}
import org.apache.spark.sql.types.{DataType => SparkDataType, TimestampNTZType}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.time.{LocalDate, LocalDateTime, LocalTime}

@SuppressWarnings(Array("org.wartremover.warts.All"))
class PredicateConverterTest extends AnyFunSpec with Matchers {

  private def colRef(name: String): Expression =
    Expressions.column(name)

  private def intLit(value: Int): Expression =
    Expressions.literal(value)

  private def longLit(value: Long): Expression =
    Expressions.literal(value)

  private def doubleLit(value: Double): Expression =
    Expressions.literal(value)

  private def floatLit(value: Float): Expression =
    Expressions.literal(value)

  private def stringLit(value: String): Expression =
    Expressions.literal(value)

  private def boolLit(value: Boolean): Expression =
    Expressions.literal(value)

  // Creates a TimestampNTZ literal carrying epoch-micros as its JVM Long value, matching the
  // representation Spark's InternalRow uses for TimestampNTZType (the same representation
  // `createExpression` receives at runtime). `Expressions.literal(value)` infers the data type
  // from the JVM type and would produce LongType for a Long, so the literal is constructed
  // directly with TimestampNTZType.
  private def timestampNtzLit(localDateTime: LocalDateTime): Expression = {
    val micros =
      localDateTime.toEpochSecond(java.time.ZoneOffset.UTC) * 1000000L +
        localDateTime.getNano.toLong / 1000L
    new Literal[java.lang.Long] {
      override def value(): java.lang.Long   = java.lang.Long.valueOf(micros)
      override def dataType(): SparkDataType = TimestampNTZType
    }
  }

  private def cmp(op: String, left: Expression, right: Expression): Predicate =
    new Predicate(op, Array[Expression](left, right))

  private val emptyColumnTypes: Map[String, ScalarDbDataType] = Map.empty

  // Converts a single comparison predicate and returns its sole leaf expression. A single
  // non-OR predicate is converted as CNF with one OR-group containing one leaf.
  private def convertLeaf(
      pred: Predicate,
      columnTypes: Map[String, ScalarDbDataType] = emptyColumnTypes
  ): ConditionalExpression = {
    val outcome = PredicateConverter.convertPredicates(Array(pred), columnTypes)
    outcome.conditions match {
      case Some(CnfConditions(orGroups)) =>
        orGroups should have size 1
        orGroups.head should have size 1
        orGroups.head.head.toConditionalExpression
      case other => fail(s"expected CNF with a single leaf, got: $other")
    }
  }

  describe("convertPredicates") {

    describe("leaf comparison operators") {
      it("should handle = with int literal") {
        val expr = convertLeaf(cmp("=", colRef("col1"), intLit(42)))
        expr.getColumn.getName shouldBe "col1"
        expr.getIntValue shouldBe 42
        expr.getOperator shouldBe Operator.EQ
      }

      it("should handle = with string literal") {
        val expr = convertLeaf(cmp("=", colRef("name"), stringLit("hello")))
        expr.getColumn.getName shouldBe "name"
        expr.getTextValue shouldBe "hello"
        expr.getOperator shouldBe Operator.EQ
      }

      it("should handle = with boolean literal") {
        val expr = convertLeaf(cmp("=", colRef("flag"), boolLit(true)))
        expr.getBooleanValue shouldBe true
        expr.getOperator shouldBe Operator.EQ
      }

      it("should handle <> (not equal)") {
        convertLeaf(cmp("<>", colRef("col1"), intLit(10))).getOperator shouldBe Operator.NE
      }

      it("should handle > (greater than)") {
        convertLeaf(cmp(">", colRef("col1"), intLit(10))).getOperator shouldBe Operator.GT
      }

      it("should handle >= (greater than or equal)") {
        convertLeaf(cmp(">=", colRef("col1"), intLit(10))).getOperator shouldBe Operator.GTE
      }

      it("should handle < (less than)") {
        convertLeaf(cmp("<", colRef("col1"), intLit(10))).getOperator shouldBe Operator.LT
      }

      it("should handle <= (less than or equal)") {
        convertLeaf(cmp("<=", colRef("col1"), intLit(10))).getOperator shouldBe Operator.LTE
      }

      it("should flip the operator when the literal is on the left side") {
        val expr = convertLeaf(cmp(">", intLit(10), colRef("col1")))
        expr.getColumn.getName shouldBe "col1"
        expr.getIntValue shouldBe 10
        expr.getOperator shouldBe Operator.LT // flipped from GT
      }
    }

    describe("null checks") {
      val nullCheckColumnTypes = Map("col1" -> ScalarDbDataType.INT)

      it("should handle IS_NULL with type-specific dispatch") {
        val pred = new Predicate("IS_NULL", Array[Expression](colRef("col1")))
        val expr = convertLeaf(pred, nullCheckColumnTypes)
        expr.getColumn.getName shouldBe "col1"
        expr.getColumn.getDataType shouldBe ScalarDbDataType.INT
        expr.getOperator shouldBe Operator.IS_NULL
      }

      it("should handle IS_NOT_NULL with type-specific dispatch") {
        val pred = new Predicate("IS_NOT_NULL", Array[Expression](colRef("col1")))
        val expr = convertLeaf(pred, nullCheckColumnTypes)
        expr.getColumn.getDataType shouldBe ScalarDbDataType.INT
        expr.getOperator shouldBe Operator.IS_NOT_NULL
      }

      it("should not push IS_NULL when the column type is unknown") {
        val pred    = new Predicate("IS_NULL", Array[Expression](colRef("unknown")))
        val outcome = PredicateConverter.convertPredicates(Array(pred), nullCheckColumnTypes)
        outcome.conditions shouldBe None
        outcome.postScan should contain theSameElementsAs Array(pred)
      }
    }

    describe("CNF mode (the predicate array is AND-combined)") {
      it("should produce one OR-group per conjunct") {
        // Spark WHERE: a = 1 AND b > 2   (Spark splits top-level ANDs into the array [a=1, b>2])
        // Pushed (CNF): [a = 1] AND [b > 2]
        val a = cmp("=", colRef("a"), intLit(1))
        val b = cmp(">", colRef("b"), intLit(2))

        val outcome = PredicateConverter.convertPredicates(Array(a, b), emptyColumnTypes)
        outcome.conditions match {
          case Some(CnfConditions(orGroups)) =>
            orGroups should have size 2
            orGroups.foreach(_ should have size 1)
            orGroups(0).head.operator shouldBe Operator.EQ
            orGroups(1).head.operator shouldBe Operator.GT
          case other => fail(s"expected CNF, got: $other")
        }
        outcome.postScan shouldBe empty
      }

      it("should map a flat OR conjunct to a single OR-group") {
        // Spark WHERE: a = 1 AND (b = 2 OR c = 3)   (array [a=1, (b OR c)])
        // Pushed (CNF): [a = 1] AND [b = 2 OR c = 3]
        val a  = cmp("=", colRef("a"), intLit(1))
        val or = new Or(cmp("=", colRef("b"), intLit(2)), cmp("=", colRef("c"), intLit(3)))

        val outcome = PredicateConverter.convertPredicates(Array(a, or), emptyColumnTypes)
        outcome.conditions match {
          case Some(CnfConditions(orGroups)) =>
            orGroups should have size 2
            orGroups(0) should have size 1 // leaf a
            orGroups(1) should have size 2 // (b OR c)
          case other => fail(s"expected CNF, got: $other")
        }
        outcome.postScan shouldBe empty
      }
    }

    describe("DNF mode (a single top-level OR predicate)") {
      it("should flatten a nested OR into individual disjuncts") {
        // Spark WHERE: a = 1 OR b = 2 OR c = 3   (a single OR predicate, nested as Or(Or(a,b), c))
        // Pushed (DNF): [a = 1] OR [b = 2] OR [c = 3]
        val or = new Or(
          new Or(cmp("=", colRef("a"), intLit(1)), cmp("=", colRef("b"), intLit(2))),
          cmp("=", colRef("c"), intLit(3))
        )
        val outcome = PredicateConverter.convertPredicates(Array(or), emptyColumnTypes)
        outcome.conditions match {
          case Some(DnfConditions(andGroups)) =>
            andGroups should have size 3
            andGroups.foreach(_ should have size 1)
          case other => fail(s"expected DNF, got: $other")
        }
      }

      it("should convert a single OR of leaves to DNF") {
        // Spark WHERE: a = 1 OR b = 2
        // Pushed (DNF): [a = 1] OR [b = 2]
        val or = new Or(cmp("=", colRef("a"), intLit(1)), cmp("=", colRef("b"), intLit(2)))

        val outcome = PredicateConverter.convertPredicates(Array(or), emptyColumnTypes)
        outcome.conditions match {
          case Some(DnfConditions(andGroups)) =>
            andGroups should have size 2
            andGroups.foreach(_ should have size 1)
          case other => fail(s"expected DNF, got: $other")
        }
        outcome.pushed should have size 1
        outcome.postScan shouldBe empty
      }

      it("should convert OR-of-ANDs to DNF AND-groups") {
        // Spark WHERE: (a = 1 AND b = 2) OR (c = 3 AND d = 4)
        // Pushed (DNF): [a = 1 AND b = 2] OR [c = 3 AND d = 4]
        val and1 = new And(cmp("=", colRef("a"), intLit(1)), cmp("=", colRef("b"), intLit(2)))
        val and2 = new And(cmp("=", colRef("c"), intLit(3)), cmp("=", colRef("d"), intLit(4)))
        val or   = new Or(and1, and2)

        val outcome = PredicateConverter.convertPredicates(Array(or), emptyColumnTypes)
        outcome.conditions match {
          case Some(DnfConditions(andGroups)) =>
            andGroups should have size 2
            andGroups.foreach(_ should have size 2)
          case other => fail(s"expected DNF, got: $other")
        }
      }

      it("should flatten a nested AND within a DNF disjunct") {
        // Spark WHERE: (a = 1 AND b = 2 AND c = 3) OR d = 4   (the AND nests as And(And(a,b),c))
        // Pushed (DNF): [a = 1 AND b = 2 AND c = 3] OR [d = 4]
        val and = new And(
          new And(cmp("=", colRef("a"), intLit(1)), cmp("=", colRef("b"), intLit(2))),
          cmp("=", colRef("c"), intLit(3))
        )
        val or = new Or(and, cmp("=", colRef("d"), intLit(4)))

        val outcome = PredicateConverter.convertPredicates(Array(or), emptyColumnTypes)
        outcome.conditions match {
          case Some(DnfConditions(andGroups)) =>
            andGroups should have size 2
            andGroups(0) should have size 3 // a AND b AND c flattened
            andGroups(1) should have size 1 // d
          case other => fail(s"expected DNF, got: $other")
        }
      }
    }

    describe("predicates that are not (fully) pushed down") {
      it("should not push a single OR whose disjunct uses an unsupported operator (IN)") {
        // Spark WHERE: a = 1 OR b IN (1, 2)   (pushes nothing)
        // `a = 1` is convertible, but `IN` is not a supported operator. Because DNF push down is
        // all-or-nothing (a disjunct cannot be dropped), the whole OR is left to Spark.
        val unsupported = new Predicate("IN", Array[Expression](colRef("b"), intLit(1), intLit(2)))
        val or          = new Or(cmp("=", colRef("a"), intLit(1)), unsupported)

        val outcome = PredicateConverter.convertPredicates(Array(or), emptyColumnTypes)
        outcome.conditions shouldBe None
        outcome.postScan should contain theSameElementsAs Array(or)
      }

      it("should push a convertible conjunct and leave one with an unsupported operator to Spark") {
        // Spark WHERE: a = 1 AND b IN (1, 2)   (array [a=1, b IN (..)])
        // Pushed (CNF): [a = 1];  deferred to Spark: b IN (1, 2)
        // In CNF mode the array elements are AND-combined, so each conjunct can be pushed
        // independently: `a = 1` is pushed, while `IN` (unsupported) is returned for Spark to
        // evaluate as a post-filter. This partial push down is safe because applying a subset of
        // ANDed conditions only narrows the rows ScalarDB returns.
        val supported   = cmp("=", colRef("a"), intLit(1))
        val unsupported = new Predicate("IN", Array[Expression](colRef("b"), intLit(1), intLit(2)))

        val outcome =
          PredicateConverter.convertPredicates(Array(supported, unsupported), emptyColumnTypes)
        outcome.conditions match {
          case Some(CnfConditions(orGroups)) => orGroups should have size 1
          case other                         => fail(s"expected CNF, got: $other")
        }
        outcome.pushed should contain theSameElementsAs Array(supported)
        outcome.postScan should contain theSameElementsAs Array(unsupported)
      }

      it("should defer a conjunct whose OR contains a nested AND") {
        // Spark WHERE: d = 4 AND ((a = 1 AND b = 2) OR c = 3)   (array [d=4, ((a AND b) OR c)])
        // Pushed (CNF): [d = 4];  deferred to Spark: (a = 1 AND b = 2) OR c = 3
        // The OR conjunct cannot become a flat OR-group (it contains a nested AND), and CNF mode
        // would require distribution, which we do not perform; so it is left to Spark.
        val dnfShaped = new Or(
          new And(cmp("=", colRef("a"), intLit(1)), cmp("=", colRef("b"), intLit(2))),
          cmp("=", colRef("c"), intLit(3))
        )
        val other = cmp("=", colRef("d"), intLit(4))

        val outcome =
          PredicateConverter.convertPredicates(Array(other, dnfShaped), emptyColumnTypes)
        outcome.conditions match {
          case Some(CnfConditions(orGroups)) => orGroups should have size 1 // only `other`
          case c                             => fail(s"expected CNF, got: $c")
        }
        outcome.pushed should contain theSameElementsAs Array(other)
        outcome.postScan should contain theSameElementsAs Array(dnfShaped)
      }

      it("should not push IN predicates") {
        // Spark WHERE: a IN (1, 2)   (IN is not a supported operator; pushes nothing)
        val pred    = new Predicate("IN", Array[Expression](colRef("a"), intLit(1), intLit(2)))
        val outcome = PredicateConverter.convertPredicates(Array(pred), emptyColumnTypes)
        outcome.conditions shouldBe None
        outcome.postScan should contain theSameElementsAs Array(pred)
      }

      it("should not push LIKE predicates") {
        // Spark WHERE: a LIKE '%foo%'   (LIKE is not a supported operator; pushes nothing)
        val pred    = new Predicate("LIKE", Array[Expression](colRef("a"), stringLit("%foo%")))
        val outcome = PredicateConverter.convertPredicates(Array(pred), emptyColumnTypes)
        outcome.conditions shouldBe None
      }
    }

    describe("data type coverage") {
      it("should handle long literal") {
        convertLeaf(cmp("=", colRef("col"), longLit(123456789L))).getBigIntValue shouldBe 123456789L
      }

      it("should handle float literal") {
        convertLeaf(cmp("=", colRef("col"), floatLit(1.5f))).getFloatValue shouldBe 1.5f
      }

      it("should handle double literal") {
        convertLeaf(cmp("=", colRef("col"), doubleLit(3.14))).getDoubleValue shouldBe 3.14
      }
    }

    // ScalarDB DataType.Time and DataType.Timestamp both map to Spark TimestampNTZType (see
    // SchemaConverter), so PredicateConverter must consult the ScalarDB column type to choose the
    // correct ConditionalExpression dispatch. The TIME branch additionally restricts push down to
    // literals whose date part is the EPOCH_DATE (1970-01-01) convention used by RowConverter for
    // TIME round-trip, since otherwise the predicate would not be equivalent to Spark's evaluation.
    describe("TimestampNTZ literal dispatch by ScalarDB column type") {
      val timeColumnTypes      = Map("time_col" -> ScalarDbDataType.TIME)
      val timestampColumnTypes = Map("ts_col" -> ScalarDbDataType.TIMESTAMP)

      it("should push a TimestampNTZ literal as TIMESTAMP for a TIMESTAMP column") {
        val literal = LocalDateTime.of(2024, 6, 2, 10, 30, 0)
        val expr =
          convertLeaf(cmp(">", colRef("ts_col"), timestampNtzLit(literal)), timestampColumnTypes)
        expr.getColumn.getDataType shouldBe ScalarDbDataType.TIMESTAMP
        expr.getTimestampValue shouldBe literal
        expr.getOperator shouldBe Operator.GT
      }

      it("should push a TimestampNTZ literal at epoch date as TIME for a TIME column") {
        val literal = LocalDateTime.of(LocalDate.ofEpochDay(0), LocalTime.of(10, 30, 0))
        val expr =
          convertLeaf(cmp(">", colRef("time_col"), timestampNtzLit(literal)), timeColumnTypes)
        expr.getColumn.getDataType shouldBe ScalarDbDataType.TIME
        expr.getTimeValue shouldBe LocalTime.of(10, 30, 0)
        expr.getOperator shouldBe Operator.GT
      }

      it("should reject a TimestampNTZ literal at non-epoch date for a TIME column") {
        // A non-EPOCH literal date can never match any TIME row (they are always at 1970-01-01),
        // so leaving such a query to silently return zero rows would hide a likely user mistake.
        // PredicateConverter raises an IllegalArgumentException at planning time so the encoding
        // constraint is reported to the user instead.
        val literal = LocalDateTime.of(2024, 6, 2, 10, 30, 0)
        val pred    = cmp(">", colRef("time_col"), timestampNtzLit(literal))
        val ex = intercept[IllegalArgumentException] {
          PredicateConverter.convertPredicates(Array(pred), timeColumnTypes)
        }
        ex.getMessage should include("time_col")
        ex.getMessage should include("1970-01-01")
      }

      it(
        "should fail loudly when a TimestampNTZ literal targets a column with an unexpected type"
      ) {
        // SchemaConverter maps only DataType.Time and DataType.Timestamp to TimestampNTZType, so
        // reaching this branch with any other ScalarDB type (or with the column missing from
        // columnTypes entirely) means schema and columnTypes disagree — a programming error
        // upstream. createExpression refuses to silently coerce in that case.
        val literal = LocalDateTime.of(1970, 1, 1, 10, 30, 0)
        val pred    = cmp(">", colRef("unknown"), timestampNtzLit(literal))
        val ex = intercept[IllegalStateException] {
          PredicateConverter.convertPredicates(Array(pred), emptyColumnTypes)
        }
        ex.getMessage should include("unknown")
      }
    }
  }
}
