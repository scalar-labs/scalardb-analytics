/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.scalardb.table

import com.scalar.db.api.{ConditionBuilder, ConditionalExpression}
import com.scalar.db.api.ConditionalExpression.Operator
import com.scalar.db.io.DataType
import org.apache.spark.sql.types.StructType

import java.time.{Instant, LocalDate, LocalDateTime, LocalTime}

/** Serializable representation of push down information that can be transmitted from the driver to
  * executors via [[ScalarDbPartitionReaderFactory]].
  */
case class PushDownInfo(
    conditions: Option[PushDownConditions],
    prunedColumnNames: Option[Array[String]],
    limit: Option[Int]
)

/** Serializable, grouped representation of the conditions to push down.
  *
  * Mirrors ScalarDB's two-level condition model: leaves grouped once, then combined. This is the
  * single condition type used from conversion on the driver ([[PredicateConverter]]) through to
  * reconstruction on the executor ([[ScalarDbPartitionReader]]).
  */
@SuppressWarnings(
  Array(
    "org.wartremover.warts.Product",
    "org.wartremover.warts.Serializable",
    "org.wartremover.warts.JavaSerializable"
  )
)
sealed trait PushDownConditions

/** CNF: OR-groups combined with AND. Applied via `Scan.whereAnd(Set[OrConditionSet])`. */
final case class CnfConditions(orGroups: Seq[Seq[SerializablePredicate]]) extends PushDownConditions

/** DNF: AND-groups combined with OR. Applied via `Scan.whereOr(Set[AndConditionSet])`. */
final case class DnfConditions(andGroups: Seq[Seq[SerializablePredicate]])
    extends PushDownConditions

/** A serializable representation of a single ScalarDB [[ConditionalExpression]].
  *
  * This holds the same information as a [[ConditionalExpression]] (column name, value, data type,
  * operator) — the two are isomorphic. It exists because [[ConditionalExpression]] and its internal
  * `Column` are not [[Serializable]], so they cannot be distributed from the driver to executors as
  * part of [[PushDownInfo]]. [[PredicateConverter]] builds these directly on the driver, and the
  * executor reconstructs the ScalarDB [[ConditionalExpression]] via [[toConditionalExpression]]
  * using [[ConditionBuilder]].
  */
@SuppressWarnings(
  Array(
    "org.wartremover.warts.Product",
    "org.wartremover.warts.Serializable",
    "org.wartremover.warts.JavaSerializable"
  )
)
case class SerializablePredicate(
    columnName: String,
    value: AnyRef,
    dataType: DataType,
    operator: Operator
) {

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  def toConditionalExpression: ConditionalExpression = {
    val builder = ConditionBuilder.column(columnName)
    operator match {
      case Operator.IS_NULL => SerializablePredicate.dispatchNull(builder, dataType, isNull = true)
      case Operator.IS_NOT_NULL =>
        SerializablePredicate.dispatchNull(builder, dataType, isNull = false)
      case _ =>
        SerializablePredicate.dispatchByType(builder, value, dataType, operator)
    }
  }
}

object SerializablePredicate {

  private[table] def dispatchNull(
      builder: ConditionBuilder.ConditionalExpressionBuilder,
      dataType: DataType,
      isNull: Boolean
  ): ConditionalExpression =
    dataType match {
      case DataType.BOOLEAN =>
        if (isNull) builder.isNullBoolean() else builder.isNotNullBoolean()
      case DataType.INT =>
        if (isNull) builder.isNullInt() else builder.isNotNullInt()
      case DataType.BIGINT =>
        if (isNull) builder.isNullBigInt() else builder.isNotNullBigInt()
      case DataType.FLOAT =>
        if (isNull) builder.isNullFloat() else builder.isNotNullFloat()
      case DataType.DOUBLE =>
        if (isNull) builder.isNullDouble() else builder.isNotNullDouble()
      case DataType.TEXT =>
        if (isNull) builder.isNullText() else builder.isNotNullText()
      case DataType.BLOB =>
        if (isNull) builder.isNullBlob() else builder.isNotNullBlob()
      case DataType.DATE =>
        if (isNull) builder.isNullDate() else builder.isNotNullDate()
      case DataType.TIME =>
        if (isNull) builder.isNullTime() else builder.isNotNullTime()
      case DataType.TIMESTAMP =>
        if (isNull) builder.isNullTimestamp() else builder.isNotNullTimestamp()
      case DataType.TIMESTAMPTZ =>
        if (isNull) builder.isNullTimestampTZ() else builder.isNotNullTimestampTZ()
      case _ =>
        throw new IllegalArgumentException(
          s"Unsupported data type for null check: ${dataType.toString}"
        )
    }

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  private[table] def dispatchByType(
      builder: ConditionBuilder.ConditionalExpressionBuilder,
      value: AnyRef,
      dataType: DataType,
      operator: Operator
  ): ConditionalExpression =
    dataType match {
      case DataType.BOOLEAN =>
        dispatchBoolean(builder, value.asInstanceOf[Boolean], operator)
      case DataType.INT =>
        dispatchInt(builder, value.asInstanceOf[Int], operator)
      case DataType.BIGINT =>
        dispatchBigInt(builder, value.asInstanceOf[Long], operator)
      case DataType.FLOAT =>
        dispatchFloat(builder, value.asInstanceOf[Float], operator)
      case DataType.DOUBLE =>
        dispatchDouble(builder, value.asInstanceOf[Double], operator)
      case DataType.TEXT =>
        dispatchText(builder, value.asInstanceOf[String], operator)
      case DataType.BLOB =>
        dispatchBlob(builder, value.asInstanceOf[Array[Byte]], operator)
      case DataType.DATE =>
        dispatchDate(builder, value.asInstanceOf[LocalDate], operator)
      case DataType.TIME =>
        dispatchTime(builder, value.asInstanceOf[LocalTime], operator)
      case DataType.TIMESTAMP =>
        dispatchTimestamp(builder, value.asInstanceOf[LocalDateTime], operator)
      case DataType.TIMESTAMPTZ =>
        dispatchTimestampTZ(builder, value.asInstanceOf[Instant], operator)
      case _ =>
        throw new IllegalArgumentException(s"Unsupported data type: ${dataType.toString}")
    }

  private[table] def dispatchBoolean(
      b: ConditionBuilder.ConditionalExpressionBuilder,
      v: Boolean,
      op: Operator
  ): ConditionalExpression = op match {
    case Operator.EQ  => b.isEqualToBoolean(v)
    case Operator.NE  => b.isNotEqualToBoolean(v)
    case Operator.GT  => b.isGreaterThanBoolean(v)
    case Operator.GTE => b.isGreaterThanOrEqualToBoolean(v)
    case Operator.LT  => b.isLessThanBoolean(v)
    case Operator.LTE => b.isLessThanOrEqualToBoolean(v)
    case _ =>
      throw new IllegalArgumentException(s"Unsupported operator for Boolean: ${op.toString}")
  }

  private[table] def dispatchInt(
      b: ConditionBuilder.ConditionalExpressionBuilder,
      v: Int,
      op: Operator
  ): ConditionalExpression = op match {
    case Operator.EQ  => b.isEqualToInt(v)
    case Operator.NE  => b.isNotEqualToInt(v)
    case Operator.GT  => b.isGreaterThanInt(v)
    case Operator.GTE => b.isGreaterThanOrEqualToInt(v)
    case Operator.LT  => b.isLessThanInt(v)
    case Operator.LTE => b.isLessThanOrEqualToInt(v)
    case _ => throw new IllegalArgumentException(s"Unsupported operator for Int: ${op.toString}")
  }

  private[table] def dispatchBigInt(
      b: ConditionBuilder.ConditionalExpressionBuilder,
      v: Long,
      op: Operator
  ): ConditionalExpression = op match {
    case Operator.EQ  => b.isEqualToBigInt(v)
    case Operator.NE  => b.isNotEqualToBigInt(v)
    case Operator.GT  => b.isGreaterThanBigInt(v)
    case Operator.GTE => b.isGreaterThanOrEqualToBigInt(v)
    case Operator.LT  => b.isLessThanBigInt(v)
    case Operator.LTE => b.isLessThanOrEqualToBigInt(v)
    case _ => throw new IllegalArgumentException(s"Unsupported operator for BigInt: ${op.toString}")
  }

  private[table] def dispatchFloat(
      b: ConditionBuilder.ConditionalExpressionBuilder,
      v: Float,
      op: Operator
  ): ConditionalExpression = op match {
    case Operator.EQ  => b.isEqualToFloat(v)
    case Operator.NE  => b.isNotEqualToFloat(v)
    case Operator.GT  => b.isGreaterThanFloat(v)
    case Operator.GTE => b.isGreaterThanOrEqualToFloat(v)
    case Operator.LT  => b.isLessThanFloat(v)
    case Operator.LTE => b.isLessThanOrEqualToFloat(v)
    case _ => throw new IllegalArgumentException(s"Unsupported operator for Float: ${op.toString}")
  }

  private[table] def dispatchDouble(
      b: ConditionBuilder.ConditionalExpressionBuilder,
      v: Double,
      op: Operator
  ): ConditionalExpression = op match {
    case Operator.EQ  => b.isEqualToDouble(v)
    case Operator.NE  => b.isNotEqualToDouble(v)
    case Operator.GT  => b.isGreaterThanDouble(v)
    case Operator.GTE => b.isGreaterThanOrEqualToDouble(v)
    case Operator.LT  => b.isLessThanDouble(v)
    case Operator.LTE => b.isLessThanOrEqualToDouble(v)
    case _ => throw new IllegalArgumentException(s"Unsupported operator for Double: ${op.toString}")
  }

  private[table] def dispatchText(
      b: ConditionBuilder.ConditionalExpressionBuilder,
      v: String,
      op: Operator
  ): ConditionalExpression = op match {
    case Operator.EQ  => b.isEqualToText(v)
    case Operator.NE  => b.isNotEqualToText(v)
    case Operator.GT  => b.isGreaterThanText(v)
    case Operator.GTE => b.isGreaterThanOrEqualToText(v)
    case Operator.LT  => b.isLessThanText(v)
    case Operator.LTE => b.isLessThanOrEqualToText(v)
    case _ => throw new IllegalArgumentException(s"Unsupported operator for Text: ${op.toString}")
  }

  private[table] def dispatchBlob(
      b: ConditionBuilder.ConditionalExpressionBuilder,
      v: Array[Byte],
      op: Operator
  ): ConditionalExpression = op match {
    case Operator.EQ  => b.isEqualToBlob(v)
    case Operator.NE  => b.isNotEqualToBlob(v)
    case Operator.GT  => b.isGreaterThanBlob(v)
    case Operator.GTE => b.isGreaterThanOrEqualToBlob(v)
    case Operator.LT  => b.isLessThanBlob(v)
    case Operator.LTE => b.isLessThanOrEqualToBlob(v)
    case _ => throw new IllegalArgumentException(s"Unsupported operator for Blob: ${op.toString}")
  }

  private[table] def dispatchDate(
      b: ConditionBuilder.ConditionalExpressionBuilder,
      v: LocalDate,
      op: Operator
  ): ConditionalExpression = op match {
    case Operator.EQ  => b.isEqualToDate(v)
    case Operator.NE  => b.isNotEqualToDate(v)
    case Operator.GT  => b.isGreaterThanDate(v)
    case Operator.GTE => b.isGreaterThanOrEqualToDate(v)
    case Operator.LT  => b.isLessThanDate(v)
    case Operator.LTE => b.isLessThanOrEqualToDate(v)
    case _ => throw new IllegalArgumentException(s"Unsupported operator for Date: ${op.toString}")
  }

  private[table] def dispatchTime(
      b: ConditionBuilder.ConditionalExpressionBuilder,
      v: LocalTime,
      op: Operator
  ): ConditionalExpression = op match {
    case Operator.EQ  => b.isEqualToTime(v)
    case Operator.NE  => b.isNotEqualToTime(v)
    case Operator.GT  => b.isGreaterThanTime(v)
    case Operator.GTE => b.isGreaterThanOrEqualToTime(v)
    case Operator.LT  => b.isLessThanTime(v)
    case Operator.LTE => b.isLessThanOrEqualToTime(v)
    case _ => throw new IllegalArgumentException(s"Unsupported operator for Time: ${op.toString}")
  }

  private[table] def dispatchTimestamp(
      b: ConditionBuilder.ConditionalExpressionBuilder,
      v: LocalDateTime,
      op: Operator
  ): ConditionalExpression = op match {
    case Operator.EQ  => b.isEqualToTimestamp(v)
    case Operator.NE  => b.isNotEqualToTimestamp(v)
    case Operator.GT  => b.isGreaterThanTimestamp(v)
    case Operator.GTE => b.isGreaterThanOrEqualToTimestamp(v)
    case Operator.LT  => b.isLessThanTimestamp(v)
    case Operator.LTE => b.isLessThanOrEqualToTimestamp(v)
    case _ =>
      throw new IllegalArgumentException(s"Unsupported operator for Timestamp: ${op.toString}")
  }

  private[table] def dispatchTimestampTZ(
      b: ConditionBuilder.ConditionalExpressionBuilder,
      v: Instant,
      op: Operator
  ): ConditionalExpression = op match {
    case Operator.EQ  => b.isEqualToTimestampTZ(v)
    case Operator.NE  => b.isNotEqualToTimestampTZ(v)
    case Operator.GT  => b.isGreaterThanTimestampTZ(v)
    case Operator.GTE => b.isGreaterThanOrEqualToTimestampTZ(v)
    case Operator.LT  => b.isLessThanTimestampTZ(v)
    case Operator.LTE => b.isLessThanOrEqualToTimestampTZ(v)
    case _ =>
      throw new IllegalArgumentException(s"Unsupported operator for TimestampTZ: ${op.toString}")
  }
}

object PushDownInfo {

  val empty: PushDownInfo = PushDownInfo(None, None, None)

  def from(
      conditions: Option[PushDownConditions],
      prunedSchema: Option[StructType],
      pushedLimit: Option[Int]
  ): PushDownInfo =
    PushDownInfo(
      conditions = conditions,
      prunedColumnNames = prunedSchema.map(_.fieldNames),
      limit = pushedLimit
    )
}
