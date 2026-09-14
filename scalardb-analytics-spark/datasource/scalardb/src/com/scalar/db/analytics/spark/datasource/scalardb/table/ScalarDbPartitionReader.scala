/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.scalardb.table

import com.scalar.db.api.TransactionManagerCrudOperable.Scanner
import com.scalar.db.api.{ConditionSetBuilder, DistributedTransactionManager, Result, Scan}
import com.scalar.db.exception.transaction.{CrudException, UnknownTransactionStatusException}
import com.scalar.db.service.TransactionFactory
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.connector.read.PartitionReader
import org.slf4j.LoggerFactory

import java.util.Properties
import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

class ScalarDbPartitionReader private (
    manager: DistributedTransactionManager,
    converter: RowConverter,
    scanner: Scanner
) extends PartitionReader[InternalRow] {

  private val logger = LoggerFactory.getLogger(classOf[ScalarDbPartitionReader])

  @SuppressWarnings(Array("org.wartremover.warts.Var", "org.wartremover.warts.Null"))
  private var currentResult: Result = _

  override def next(): Boolean =
    try {
      val result = scanner.one()
      if (result.isPresent) {
        currentResult = result.get()
        true
      } else {
        false
      }
    } catch {
      case e: CrudException =>
        throw new ScalarDbException("Failed to get next result from scanner", e)
    }

  override def get(): InternalRow =
    converter.convertToInternalRow(currentResult)

  override def close(): Unit =
    try
      scanner.close()
    // ScalarDb's scanner.close() currently throw UnknownTransactionStatusException, but this may
    // be changed in the future because the ScalarDB team is now planning to optimize the
    // coordination process by skipping the coordinate writing for read-only transactions.
    // If it is changed, we can remove the catch block for UnknownTransactionStatusException.
    // See: https://github.com/scalar-labs/scalardb-analytics/pull/129#discussion_r2149704900
    catch {
      case e @ (_: CrudException | _: UnknownTransactionStatusException) =>
        logger.warn(
          "Failed to close the scanner. However, we will proceed to close the transaction manager." +
            " Since only read-only operations were performed, there is no risk to data consistency. Details: {}",
          e.getMessage
        )
    } finally
      manager.close()
}

object ScalarDbPartitionReader {
  def create(
      partition: ScalarDbInputPartition,
      configProperties: Properties,
      desc: ScalarDbTableDescription,
      pushDownInfo: PushDownInfo
  ): ScalarDbPartitionReader = {
    // Note: partition parameter will be used for partition-based filtering once implemented
    val _       = partition
    val factory = TransactionFactory.create(configProperties)
    val manager = factory.getTransactionManager
    try {
      val converter = RowConverter.create(desc, pushDownInfo.prunedColumnNames)
      val scan      = buildScan(desc, pushDownInfo)
      val scanner   = manager.getScanner(scan)
      new ScalarDbPartitionReader(manager, converter, scanner)
    } catch {
      case e: CrudException =>
        manager.close()
        throw new ScalarDbException("Failed to get scanner", e)
      case NonFatal(e) =>
        manager.close()
        throw new ScalarDbException("Failed to create ScalarDbPartitionReader", e)
    }
  }

  private def buildScan(desc: ScalarDbTableDescription, pushDownInfo: PushDownInfo): Scan = {
    val baseBuilder = Scan.newBuilder().namespace(desc.namespace).table(desc.table).all()

    // Apply projections and limit on the base builder before where().
    // These are preserved because BuildableScanAllWithWhere delegates to the same BuildableScanAll.
    pushDownInfo.prunedColumnNames.foreach(_.foreach(baseBuilder.projection))
    pushDownInfo.limit.foreach(baseBuilder.limit)

    // Apply conditions as CNF (whereAnd of OR-groups) or DNF (whereOr of AND-groups). ScalarDB
    // guarantees correct filtering regardless of backend push-down support, so any representable
    // condition is safe to apply here.
    pushDownInfo.conditions match {
      case None =>
        baseBuilder.build()
      case Some(CnfConditions(orGroups)) =>
        val orConditionSets = orGroups.map { group =>
          ConditionSetBuilder
            .orConditionSet(group.map(_.toConditionalExpression).toSet.asJava)
            .build()
        }.toSet
        baseBuilder.whereAnd(orConditionSets.asJava).build()
      case Some(DnfConditions(andGroups)) =>
        val andConditionSets = andGroups.map { group =>
          ConditionSetBuilder
            .andConditionSet(group.map(_.toConditionalExpression).toSet.asJava)
            .build()
        }.toSet
        baseBuilder.whereOr(andConditionSets.asJava).build()
    }
  }
}
