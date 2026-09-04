/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.scalardb.table

import com.scalar.db.analytics.api.model.TableDetail
import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider
import com.scalar.db.analytics.spark.datasource.scalardb.config.{DatabaseConfigUtil, StorageType}
import com.scalar.db.analytics.spark.lib.catalog.CatalogIdentifier
import com.scalar.db.config.DatabaseConfig
import com.scalar.db.transaction.consensuscommit.{ConsensusCommitConfig, Isolation}
import java.io.IOException
import java.util.Properties

object ScalarDbTableFactory {
  private val ScanFetchSize = "4096"

  def create(
      identifier: CatalogIdentifier,
      table: TableDetail,
      provider: ScalarDbProvider
  ): ScalarDbTable =
    try {
      val properties = provider.toProperties
      ensureScanFetchSize(properties)
      forceCrossPartitionScan(properties)
      val config = forceReadCommitted(new DatabaseConfig(properties))
      createImpl(config, identifier, table)
    } catch {
      case e: IOException =>
        throw new ScalarDbException("Failed to create a Table for ScalarDB.", e)
    }

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def createImpl(
      config: DatabaseConfig,
      identifier: CatalogIdentifier,
      table: TableDetail
  ): ScalarDbTable =
    StorageType.of(config.getStorage) match {
      case Some(StorageType.MultiStorage) =>
        createImpl(
          DatabaseConfigUtil.getConfigForNamespace(
            config,
            NamespaceUtil.getScalarDbNamespace(identifier)
          ),
          identifier,
          table
        )
      case Some(_) =>
        new ScalarDbTable(identifier, table, config)
      case None =>
        throw new IllegalArgumentException(s"Unknown ScalarDB storage type: ${config.getStorage}")
    }

  private def ensureScanFetchSize(properties: Properties): Unit = {
    val _ = properties.putIfAbsent(DatabaseConfig.SCAN_FETCH_SIZE, ScanFetchSize)
  }

  /** Override cross_partition_scan.enabled and cross_partition_scan.filtering.enabled to true,
    * since ScalarDB Analytics uses ScanAll internally and push down requires filtering support.
    */
  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  private def forceCrossPartitionScan(properties: Properties): Unit = {
    properties.put(DatabaseConfig.CROSS_PARTITION_SCAN, "true")
    properties.put(DatabaseConfig.CROSS_PARTITION_SCAN_FILTERING, "true")
  }

  /** If the transaction manager is Consensus Commit, overwrite the isolation level to
    * READ_COMMITTED.
    */
  private def forceReadCommitted(config: DatabaseConfig): DatabaseConfig =
    if (config.getTransactionManager != ConsensusCommitConfig.TRANSACTION_MANAGER_NAME) {
      config
    } else {
      val properties = config.getProperties
      val _ =
        properties.put(ConsensusCommitConfig.ISOLATION_LEVEL, Isolation.READ_COMMITTED.toString)
      new DatabaseConfig(properties)
    }
}
