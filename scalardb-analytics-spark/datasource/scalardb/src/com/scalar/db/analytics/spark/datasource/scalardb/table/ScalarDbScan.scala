/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.scalardb.table

import com.scalar.db.analytics.spark.lib.catalog.CatalogIdentifier
import com.scalar.db.analytics.spark.lib.table.SchemaConverter
import com.scalar.db.api.ConditionalExpression.Operator
import com.scalar.db.config.DatabaseConfig
import org.apache.spark.sql.connector.read.{Batch, Scan}
import org.apache.spark.sql.types.StructType

class ScalarDbScan(
    private val identifier: CatalogIdentifier,
    table: com.scalar.db.analytics.api.model.TableDetail,
    config: DatabaseConfig,
    pushedConditions: Option[PushDownConditions],
    prunedSchema: Option[StructType],
    pushedLimit: Option[Int]
) extends Scan {

  override def readSchema(): StructType =
    prunedSchema.getOrElse(SchemaConverter.toSparkSchema(table))

  override def toBatch(): Batch =
    new ScalarDbBatch(
      identifier,
      table,
      config,
      pushedConditions,
      prunedSchema,
      pushedLimit
    )

  override def description(): String = {
    val parts = Seq.newBuilder[String]
    parts += super.description()
    ScalarDbScan.describeConditions(pushedConditions).foreach(parts += _)
    prunedSchema.foreach(s => parts += s"PushedColumns: [${s.fieldNames.mkString(", ")}]")
    pushedLimit.foreach(l => parts += s"PushedLimit: ${l.toString}")
    parts.result().mkString(", ")
  }

  // TODO: remove this equals/hashCode override — it is dead code. Spark's CacheManager matches
  // the pre-optimization plan (DataSourceV2Relation, compared via ScalarDbTable) before this Scan
  // is created, and physical reuse keys on BatchScanExec's Batch (ScalarDbBatch uses reference
  // equality), so Scan.equals is never consulted. Tracked together with the InputFormatScan /
  // InputFormatTableWrapper cleanup.
  override def equals(obj: Any): Boolean = obj match {
    case other: ScalarDbScan => identifier == other.identifier
    case _                   => false
  }

  override def hashCode(): Int = identifier.hashCode()
}

object ScalarDbScan {

  /** Renders pushed-down conditions for `Scan.description()`, distinguishing CNF and DNF. */
  private[table] def describeConditions(conditions: Option[PushDownConditions]): Option[String] = {
    def renderGroup(group: Seq[SerializablePredicate], sep: String): String =
      group.map(renderPredicate).mkString("(", sep, ")")

    conditions match {
      case None => None
      case Some(CnfConditions(orGroups)) =>
        val body = orGroups.map(renderGroup(_, " OR ")).mkString(" AND ")
        Some(s"PushedPredicates: [$body]")
      case Some(DnfConditions(andGroups)) =>
        val body = andGroups.map(renderGroup(_, " AND ")).mkString(" OR ")
        Some(s"PushedPredicates: [$body]")
    }
  }

  /** Renders a single predicate as `column op value` (or `column op` for null checks). */
  private def renderPredicate(p: SerializablePredicate): String =
    p.operator match {
      case Operator.IS_NULL     => s"${p.columnName} IS NULL"
      case Operator.IS_NOT_NULL => s"${p.columnName} IS NOT NULL"
      case _ => s"${p.columnName} ${renderOperator(p.operator)} ${p.value.toString}"
    }

  private def renderOperator(op: Operator): String = op match {
    case Operator.EQ  => "="
    case Operator.NE  => "<>"
    case Operator.GT  => ">"
    case Operator.GTE => ">="
    case Operator.LT  => "<"
    case Operator.LTE => "<="
    // Other operators (IS_NULL / IS_NOT_NULL handled in renderPredicate; LIKE / NOT_LIKE are never
    // pushed down) fall back to the enum name.
    case other => other.toString
  }
}
