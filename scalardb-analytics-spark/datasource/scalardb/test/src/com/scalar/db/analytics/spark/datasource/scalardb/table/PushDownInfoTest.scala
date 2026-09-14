/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.scalardb.table

import com.scalar.db.api.ConditionalExpression.Operator
import com.scalar.db.io.{DataType => ScalarDbDataType}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

@SuppressWarnings(Array("org.wartremover.warts.All"))
class PushDownInfoTest extends AnyFunSuite with Matchers {

  private def roundTrip[T <: java.io.Serializable](obj: T): T = {
    val bos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(bos)
    oos.writeObject(obj)
    oos.close()
    val bis = new ByteArrayInputStream(bos.toByteArray)
    val ois = new ObjectInputStream(bis)
    ois.readObject().asInstanceOf[T]
  }

  // --- SerializablePredicate round-trip ---

  test("SerializablePredicate should round-trip INT expression") {
    val serializable = SerializablePredicate("col1", Int.box(42), ScalarDbDataType.INT, Operator.EQ)
    val deserialized = roundTrip(serializable)
    val restored     = deserialized.toConditionalExpression

    restored.getColumn.getName shouldBe "col1"
    restored.getIntValue shouldBe 42
    restored.getOperator shouldBe Operator.EQ
  }

  test("SerializablePredicate should round-trip TEXT expression") {
    val serializable = SerializablePredicate("name", "hello", ScalarDbDataType.TEXT, Operator.NE)
    val deserialized = roundTrip(serializable)
    val restored     = deserialized.toConditionalExpression

    restored.getTextValue shouldBe "hello"
    restored.getOperator shouldBe Operator.NE
  }

  test("SerializablePredicate should round-trip BOOLEAN expression") {
    val serializable =
      SerializablePredicate("flag", Boolean.box(true), ScalarDbDataType.BOOLEAN, Operator.EQ)
    val deserialized = roundTrip(serializable)
    val restored     = deserialized.toConditionalExpression

    restored.getBooleanValue shouldBe true
  }

  test("SerializablePredicate should round-trip BIGINT expression") {
    val serializable =
      SerializablePredicate("big", Long.box(999999999L), ScalarDbDataType.BIGINT, Operator.GTE)
    val deserialized = roundTrip(serializable)
    val restored     = deserialized.toConditionalExpression

    restored.getBigIntValue shouldBe 999999999L
    restored.getOperator shouldBe Operator.GTE
  }

  test("SerializablePredicate should round-trip FLOAT expression") {
    val serializable =
      SerializablePredicate("f", Float.box(1.5f), ScalarDbDataType.FLOAT, Operator.LT)
    val deserialized = roundTrip(serializable)
    val restored     = deserialized.toConditionalExpression

    restored.getFloatValue shouldBe 1.5f
  }

  test("SerializablePredicate should round-trip DOUBLE expression") {
    val serializable =
      SerializablePredicate("d", Double.box(3.14), ScalarDbDataType.DOUBLE, Operator.LTE)
    val deserialized = roundTrip(serializable)
    val restored     = deserialized.toConditionalExpression

    restored.getDoubleValue shouldBe 3.14
  }

  test("SerializablePredicate should round-trip IS_NULL expression") {
    val serializable =
      SerializablePredicate("nullable", null, ScalarDbDataType.TEXT, Operator.IS_NULL)
    val deserialized = roundTrip(serializable)
    val restored     = deserialized.toConditionalExpression

    restored.getColumn.getName shouldBe "nullable"
    restored.getOperator shouldBe Operator.IS_NULL
  }

  test("SerializablePredicate should round-trip IS_NOT_NULL expression") {
    val serializable =
      SerializablePredicate("notnull", null, ScalarDbDataType.INT, Operator.IS_NOT_NULL)
    val deserialized = roundTrip(serializable)
    val restored     = deserialized.toConditionalExpression

    restored.getColumn.getName shouldBe "notnull"
    restored.getOperator shouldBe Operator.IS_NOT_NULL
  }

  // --- PushDownInfo round-trip ---

  test("PushDownInfo should round-trip CNF conditions, columns, and limit") {
    val a = SerializablePredicate("a", Int.box(1), ScalarDbDataType.INT, Operator.EQ)
    val b = SerializablePredicate("b", "x", ScalarDbDataType.TEXT, Operator.NE)
    val c = SerializablePredicate("c", Int.box(2), ScalarDbDataType.INT, Operator.GT)

    // CNF: a AND (b OR c)
    val info = PushDownInfo.from(
      conditions = Some(CnfConditions(Seq(Seq(a), Seq(b, c)))),
      prunedSchema = None,
      pushedLimit = Some(100)
    )

    val deserialized = roundTrip(info)
    deserialized.limit shouldBe Some(100)
    deserialized.prunedColumnNames shouldBe None
    deserialized.conditions match {
      case Some(CnfConditions(orGroups)) =>
        orGroups should have length 2
        orGroups(0) should have length 1
        orGroups(1) should have length 2
        orGroups(0)(0).toConditionalExpression.getColumn.getName shouldBe "a"
        orGroups(1).map(_.toConditionalExpression.getColumn.getName).toSet shouldBe Set("b", "c")
      case other => fail(s"expected CNF conditions, got: $other")
    }
  }

  test("PushDownInfo should round-trip DNF conditions") {
    val a = SerializablePredicate("a", Int.box(1), ScalarDbDataType.INT, Operator.EQ)
    val b = SerializablePredicate("b", Int.box(2), ScalarDbDataType.INT, Operator.EQ)
    val c = SerializablePredicate("c", Int.box(3), ScalarDbDataType.INT, Operator.EQ)

    // DNF: (a AND b) OR c
    val info = PushDownInfo.from(
      conditions = Some(DnfConditions(Seq(Seq(a, b), Seq(c)))),
      prunedSchema = None,
      pushedLimit = None
    )

    val deserialized = roundTrip(info)
    deserialized.conditions match {
      case Some(DnfConditions(andGroups)) =>
        andGroups should have length 2
        andGroups(0) should have length 2
        andGroups(1) should have length 1
      case other => fail(s"expected DNF conditions, got: $other")
    }
  }

  test("PushDownInfo.from with no conditions yields None") {
    val info = PushDownInfo.from(None, None, None)
    info.conditions shouldBe None
  }

  test("PushDownInfo.empty should be serializable") {
    val deserialized = roundTrip(PushDownInfo.empty)
    deserialized.conditions shouldBe None
    deserialized.limit shouldBe None
    deserialized.prunedColumnNames shouldBe None
  }
}
