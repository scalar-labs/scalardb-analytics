/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.scalardb.table

import com.scalar.db.api.ConditionalExpression.Operator
import org.apache.spark.sql.connector.expressions.filter.{And => SparkAnd, Or => SparkOr, Predicate}
import org.apache.spark.sql.connector.expressions.{Literal, NamedReference}
import com.scalar.db.io.{DataType => ScalarDbDataType}
import org.apache.spark.sql.types._
import org.apache.spark.unsafe.types.UTF8String

import java.time.{Instant, LocalDate, LocalDateTime}

/** Converts Spark V2 [[Predicate]] trees into ScalarDB conditions for push down.
  *
  * ==ScalarDB condition model==
  * ScalarDB's Scan API can express only a fixed two-level boolean form:
  *   - CNF via `whereAnd(Set[OrConditionSet])`: an AND of OR-groups, where each OR-group is a flat
  *     set of leaf [[ConditionalExpression]]s.
  *   - DNF via `whereOr(Set[AndConditionSet])`: an OR of AND-groups, each a flat set of leaves.
  *
  * `OrConditionSet` / `AndConditionSet` hold only leaves (a type-level constraint), so arbitrary
  * nesting cannot be represented, and a single Scan is either CNF or DNF — never a mix.
  *
  * ==What Spark hands us==
  * `pushPredicates` receives an array of predicates that are implicitly AND-combined (Spark's
  * contract). Spark's `splitConjunctivePredicates` only splits top-level ANDs into this array; it
  * does not normalize to CNF or DNF, so the user's boolean structure is preserved (an OR arrives as
  * a single nested predicate). We likewise do not normalize: we only flatten nested ORs (when
  * building an OR-group, and when splitting a single top-level OR into disjuncts) and nested ANDs
  * (when building an AND-group in DNF mode), and push down only when the result already fits the
  * two-level CNF or DNF form. The distributive law is never applied, to avoid combinatorial
  * blow-up; anything that would require distribution is left to Spark.
  *
  * ==Mode selection (see [[convertPredicates]])==
  *   - A single top-level OR predicate is a disjunction -> DNF.
  *   - Otherwise the array is a conjunction -> CNF (the natural fit for an AND-combined array).
  *
  * ==Partial push down and correctness==
  * In CNF mode each conjunct is pushed independently; conjuncts that cannot be represented are
  * returned for Spark to evaluate as post-filters. This is safe because applying a subset of ANDed
  * conditions only narrows the rows ScalarDB returns, and Spark re-applies the rest. DNF mode is
  * all-or-nothing: a disjunct cannot be dropped or weakened without changing the result, so if any
  * disjunct is not representable the whole OR is left to Spark.
  *
  * Pushing a condition to ScalarDB is correct regardless of the storage backend: ScalarDB's
  * cross-partition scan filtering (enabled by [[ScalarDbTableFactory]]) evaluates every pushed
  * condition itself when the backend cannot push it down. We therefore push any representable
  * condition without reasoning about backend capabilities.
  *
  * ==Supported operators==
  * Leaf operators =, <>, >, >=, <, <= and IS_NULL / IS_NOT_NULL are matched by `Predicate.name()`.
  * Logical AND / OR are matched by type ([[org.apache.spark.sql.connector.expressions.filter.And]]
  * and [[org.apache.spark.sql.connector.expressions.filter.Or]]). NOT and operators with no
  * ScalarDB equivalent (e.g. IN) are unsupported and fall through to Spark.
  *
  * LIKE / NOT_LIKE are deliberately not pushed down even though ScalarDB's Scan API supports them
  * (`isLikeText` / `isNotLikeText`). Pushing a filter is only safe when ScalarDB evaluates it
  * identically to Spark (see the partial-push reasoning above), but the LIKE pattern semantics
  * (wildcard and escape syntax, case sensitivity) have not been verified to match between Spark and
  * ScalarDB. They are left to Spark until that compatibility is confirmed.
  */
object PredicateConverter {

  /** Outcome of converting the full predicate array passed to `pushPredicates`.
    *
    * @param conditions
    *   the grouped conditions to push down ([[PushDownConditions]]), or None if nothing is pushable
    * @param pushed
    *   the Spark predicates that were fully handled (reported via `pushedPredicates()`)
    * @param postScan
    *   the Spark predicates that must still be evaluated by Spark
    */
  final case class ConversionOutcome(
      conditions: Option[PushDownConditions],
      pushed: Array[Predicate],
      postScan: Array[Predicate]
  )

  /** Converts the predicate array from `pushPredicates` (whose elements are AND-combined) to a
    * pushable [[PushDownConditions]] form, choosing CNF or DNF mode. See the class documentation
    * for the mode-selection rule and the partial-push / all-or-nothing reasoning.
    */
  def convertPredicates(
      predicates: Array[Predicate],
      columnTypes: Map[String, ScalarDbDataType]
  ): ConversionOutcome =
    if (predicates.length == 1 && isOr(predicates(0))) {
      val disjuncts = flattenOr(predicates(0))
      val andGroups = sequence(disjuncts.map(d => toAndGroup(d, columnTypes)))
      andGroups match {
        case Some(groups) =>
          ConversionOutcome(Some(DnfConditions(groups)), predicates, Array.empty)
        case None =>
          ConversionOutcome(None, Array.empty, predicates)
      }
    } else {
      val converted = predicates.map(p => (p, toOrGroup(p, columnTypes)))
      val pushed    = converted.collect { case (p, Some(group)) => (p, group) }
      val postScan  = converted.collect { case (p, None) => p }
      if (pushed.isEmpty) ConversionOutcome(None, Array.empty, predicates)
      else
        ConversionOutcome(
          Some(CnfConditions(pushed.map(_._2).toSeq)),
          pushed.map(_._1),
          postScan
        )
    }

  private def isOr(predicate: Predicate): Boolean = predicate match {
    case _: SparkOr => true
    case _          => false
  }

  /** Flattens nested binary ORs into a flat list of operands. A non-OR predicate yields itself. */
  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def flattenOr(predicate: Predicate): Seq[Predicate] =
    predicate match {
      case or: SparkOr => flattenOr(or.left()) ++ flattenOr(or.right())
      case other       => Seq(other)
    }

  /** Flattens nested binary ANDs into a flat list of operands. A non-AND predicate yields itself.
    */
  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def flattenAnd(predicate: Predicate): Seq[Predicate] =
    predicate match {
      case and: SparkAnd => flattenAnd(and.left()) ++ flattenAnd(and.right())
      case other         => Seq(other)
    }

  /** Converts a CNF conjunct to an OR-group of leaves. Returns None if any operand is not a
    * convertible leaf (e.g. it contains a nested AND).
    */
  private def toOrGroup(
      predicate: Predicate,
      columnTypes: Map[String, ScalarDbDataType]
  ): Option[Seq[SerializablePredicate]] =
    sequence(flattenOr(predicate).map(p => convertSingle(p, columnTypes)))

  /** Converts a DNF disjunct to an AND-group of leaves. Returns None if any operand is not a
    * convertible leaf (e.g. it contains a nested OR).
    */
  private def toAndGroup(
      predicate: Predicate,
      columnTypes: Map[String, ScalarDbDataType]
  ): Option[Seq[SerializablePredicate]] =
    sequence(flattenAnd(predicate).map(p => convertSingle(p, columnTypes)))

  /** Returns Some(all values) if every element is Some, otherwise None. */
  private def sequence[A](options: Seq[Option[A]]): Option[Seq[A]] =
    if (options.forall(_.isDefined)) Some(options.collect { case Some(a) => a }) else None

  /** Converts a single comparison predicate to a [[SerializablePredicate]].
    *
    * @return
    *   Some(predicate) if convertible, None otherwise
    */
  def convertSingle(
      predicate: Predicate,
      columnTypes: Map[String, ScalarDbDataType]
  ): Option[SerializablePredicate] =
    predicate.name() match {
      case "="           => convertComparison(predicate, Operator.EQ, columnTypes)
      case "<>"          => convertComparison(predicate, Operator.NE, columnTypes)
      case ">"           => convertComparison(predicate, Operator.GT, columnTypes)
      case ">="          => convertComparison(predicate, Operator.GTE, columnTypes)
      case "<"           => convertComparison(predicate, Operator.LT, columnTypes)
      case "<="          => convertComparison(predicate, Operator.LTE, columnTypes)
      case "IS_NULL"     => convertNullCheck(predicate, Operator.IS_NULL, columnTypes)
      case "IS_NOT_NULL" => convertNullCheck(predicate, Operator.IS_NOT_NULL, columnTypes)
      case _             => None
    }

  private def convertComparison(
      predicate: Predicate,
      operator: Operator,
      columnTypes: Map[String, ScalarDbDataType]
  ): Option[SerializablePredicate] = {
    val children = predicate.children()
    if (children.length != 2) None
    else
      (children(0), children(1)) match {
        case (ref: NamedReference, lit: Literal[_]) if ref.fieldNames().length == 1 =>
          createExpression(ref.fieldNames()(0), lit, operator, columnTypes)

        case (lit: Literal[_], ref: NamedReference) if ref.fieldNames().length == 1 =>
          createExpression(ref.fieldNames()(0), lit, flipOperator(operator), columnTypes)

        case _ => None
      }
  }

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  private def convertNullCheck(
      predicate: Predicate,
      operator: Operator,
      columnTypes: Map[String, ScalarDbDataType]
  ): Option[SerializablePredicate] = {
    val children = predicate.children()
    if (children.length != 1) None
    else
      children(0) match {
        case ref: NamedReference if ref.fieldNames().length == 1 =>
          val colName = ref.fieldNames()(0)
          columnTypes.get(colName).map { scalarDbType =>
            SerializablePredicate(colName, null, scalarDbType, operator)
          }
        case _ => None
      }
  }

  /** Builds a [[SerializablePredicate]] from a Spark literal, mapping the Spark type to a ScalarDB
    * [[ScalarDbDataType]] and the literal value to the representation expected on the executor by
    * [[SerializablePredicate.dispatchByType]] (e.g. UTF8String -> String, epoch days -> LocalDate,
    * micros -> LocalDateTime / Instant). Primitive values are auto-boxed to AnyRef.
    *
    * Push down must not change the query result. Most types satisfy this unconditionally; TIME is
    * the exception (see the `TimestampNTZType` branch below).
    */
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  private def createExpression(
      colName: String,
      lit: Literal[_],
      operator: Operator,
      columnTypes: Map[String, ScalarDbDataType]
  ): Option[SerializablePredicate] =
    if (lit.value() == null) None
    else {
      def predicate(value: AnyRef, dataType: ScalarDbDataType): Option[SerializablePredicate] =
        Some(SerializablePredicate(colName, value, dataType, operator))

      lit.dataType() match {
        case BooleanType =>
          predicate(Boolean.box(lit.value().asInstanceOf[Boolean]), ScalarDbDataType.BOOLEAN)

        case IntegerType =>
          predicate(Int.box(lit.value().asInstanceOf[Int]), ScalarDbDataType.INT)

        case LongType =>
          predicate(Long.box(lit.value().asInstanceOf[Long]), ScalarDbDataType.BIGINT)

        case FloatType =>
          predicate(Float.box(lit.value().asInstanceOf[Float]), ScalarDbDataType.FLOAT)

        case DoubleType =>
          predicate(Double.box(lit.value().asInstanceOf[Double]), ScalarDbDataType.DOUBLE)

        case StringType =>
          val str = lit.value() match {
            case utf8: UTF8String => utf8.toString
            case s: String        => s
            case other            => other.toString
          }
          predicate(str, ScalarDbDataType.TEXT)

        case BinaryType =>
          predicate(lit.value().asInstanceOf[Array[Byte]], ScalarDbDataType.BLOB)

        case DateType =>
          val days = lit.value().asInstanceOf[Int]
          predicate(LocalDate.ofEpochDay(days.toLong), ScalarDbDataType.DATE)

        case TimestampNTZType =>
          val micros  = lit.value().asInstanceOf[Long]
          val seconds = Math.floorDiv(micros, 1000000L)
          val nanoAdj = Math.floorMod(micros, 1000000L) * 1000L
          val ldt =
            LocalDateTime.ofEpochSecond(seconds, nanoAdj.toInt, java.time.ZoneOffset.UTC)
          // Both DataType.Time and DataType.Timestamp map to TimestampNTZType in SchemaConverter,
          // so the ScalarDB column type is needed to decide between the two pushes.
          columnTypes.get(colName) match {
            case Some(ScalarDbDataType.TIMESTAMP) =>
              predicate(ldt, ScalarDbDataType.TIMESTAMP)
            case Some(ScalarDbDataType.TIME) =>
              // Push down must not change the result. RowConverter surfaces TIME rows as
              // LocalDateTime(EPOCH_DATE=1970-01-01, time), so Spark's TimestampNTZ comparison
              // agrees with ScalarDB's time-of-day comparison only when the literal's date part
              // is also EPOCH_DATE. A non-EPOCH literal cannot match any row regardless of its
              // time-of-day, so the query as written is almost certainly a mistake (the user
              // likely intended to filter by time of day but supplied a real-world date). Reject
              // it at planning time with a message that names the constraint, rather than
              // returning zero rows from a full scan and leaving the user to figure out why.
              //
              // TODO: only fires for leaves reaching `createExpression`. Predicates that fall
              // through `convertSingle`'s `case _` (NOT, IN, function-wrapped columns) bypass
              // this check. Closing the gap would require adding a separate pass that walks the
              // full predicate tree (including children of unsupported operators) and validates
              // every leaf regardless of push-down decision.
              val literalAtEpochDate = Math.floorDiv(micros, MicrosPerDay) == 0L
              if (literalAtEpochDate) predicate(ldt.toLocalTime, ScalarDbDataType.TIME)
              else
                throw new IllegalArgumentException(
                  s"Cannot push down a comparison on TIME column '$colName' with a TimestampNTZ " +
                    s"literal whose date part is not 1970-01-01 (got: ${ldt.toLocalDate.toString}). " +
                    "TIME values are encoded as TimestampNTZ at 1970-01-01 in this connector; use a " +
                    "literal with date 1970-01-01 to filter by time of day."
                )
            case other =>
              val otherStr = other.toString
              throw new IllegalStateException(
                s"Unexpected ScalarDB type for Spark TimestampNTZ column '$colName': $otherStr " +
                  "(expected DataType.Time or DataType.Timestamp; check SchemaConverter and " +
                  "the columnTypes map passed to PredicateConverter)"
              )
          }

        case TimestampType =>
          val micros  = lit.value().asInstanceOf[Long]
          val seconds = Math.floorDiv(micros, 1000000L)
          val nanoAdj = Math.floorMod(micros, 1000000L) * 1000L
          predicate(Instant.ofEpochSecond(seconds, nanoAdj), ScalarDbDataType.TIMESTAMPTZ)

        case _ => None
      }
    }

  private val MicrosPerDay: Long = 86400L * 1000L * 1000L

  private def flipOperator(op: Operator): Operator = op match {
    case Operator.GT  => Operator.LT
    case Operator.GTE => Operator.LTE
    case Operator.LT  => Operator.GT
    case Operator.LTE => Operator.GTE
    case other        => other
  }
}
