/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.scalardb.table

import com.scalar.db.analytics.spark.lib.catalog.CatalogIdentifier
import com.scalar.db.config.DatabaseConfig
import com.scalar.db.io.{DataType => ScalarDbDataType}
import org.apache.spark.sql.connector.expressions.filter.Predicate
import org.apache.spark.sql.connector.read.{
  Scan,
  ScanBuilder,
  SupportsPushDownLimit,
  SupportsPushDownRequiredColumns,
  SupportsPushDownV2Filters
}
import org.apache.spark.sql.types.StructType

import scala.jdk.CollectionConverters._

class ScalarDbScanBuilder(
    identifier: CatalogIdentifier,
    table: com.scalar.db.analytics.api.model.TableDetail,
    config: DatabaseConfig
) extends ScanBuilder
    with SupportsPushDownV2Filters
    with SupportsPushDownRequiredColumns
    with SupportsPushDownLimit {

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var pushedConditions: Option[PushDownConditions] = None

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var pushedPreds: Array[Predicate] = Array.empty

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var prunedSchema: Option[StructType] = None

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var pushedLimitValue: Option[Int] = None

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  private lazy val columnTypes: Map[String, ScalarDbDataType] =
    Option(table)
      .map(_.getColumns.asScala.map(c => c.getName -> TypeMapping.toScalarDbType(c.getType)).toMap)
      .getOrElse(Map.empty[String, ScalarDbDataType])

  override def pushPredicates(predicates: Array[Predicate]): Array[Predicate] = {
    val outcome = PredicateConverter.convertPredicates(predicates, columnTypes)
    pushedConditions = outcome.conditions
    pushedPreds = outcome.pushed
    outcome.postScan
  }

  override def pushedPredicates(): Array[Predicate] = pushedPreds

  override def pruneColumns(requiredSchema: StructType): Unit =
    prunedSchema = Some(requiredSchema)

  // Returns true because ScalarDB's Scan.limit() enforces a global limit, and we
  // currently use a single partition (ScalarDbBatch), so Spark can safely remove its
  // own Limit operator. If multi-partition support is added in the future, override
  // isPartiallyPushed() to return true so that Spark retains its Limit operator to
  // enforce the global limit across partitions.
  override def pushLimit(limit: Int): Boolean = {
    pushedLimitValue = Some(limit)
    true
  }

  override def build(): Scan =
    new ScalarDbScan(
      identifier,
      table,
      config,
      pushedConditions,
      prunedSchema,
      pushedLimitValue
    )
}
