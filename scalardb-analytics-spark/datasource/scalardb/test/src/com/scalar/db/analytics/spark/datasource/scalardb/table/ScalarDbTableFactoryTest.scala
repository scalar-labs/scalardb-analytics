/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.scalardb.table

import com.scalar.db.analytics.api.model.{Column, DataType, TableDetail, TableInfo}
import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider
import com.scalar.db.analytics.spark.lib.catalog.{CatalogIdentifier, CatalogNamespace}
import com.scalar.db.config.DatabaseConfig
import com.scalar.db.storage.jdbc.JdbcConfig
import com.scalar.db.transaction.consensuscommit.{ConsensusCommitConfig, Isolation}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.OptionValues

import java.util.{Collections, UUID}

import scala.jdk.CollectionConverters._

class ScalarDbTableFactoryTest extends AnyFunSuite with Matchers with OptionValues {

  private val identifier =
    CatalogIdentifier(CatalogNamespace("ds", List("ns")), "tbl")

  private val tableDetail = new TableDetail(
    TableInfo.create(UUID.randomUUID(), "tbl"),
    Collections.singletonList(
      Column.create(UUID.randomUUID(), "col", DataType.Int.INSTANCE, 0, false)
    )
  )

  private def provider(configs: Map[String, String]): ScalarDbProvider =
    new ScalarDbProvider(configs.asJava)

  private def createAndGetConfig(configs: Map[String, String]): DatabaseConfig =
    ScalarDbTableFactory.create(identifier, tableDetail, provider(configs)).config

  private val baseConfigs: Map[String, String] = Map(
    DatabaseConfig.CONTACT_POINTS -> "localhost",
    DatabaseConfig.STORAGE        -> "jdbc"
  )

  test("create should set scan fetch size to 4096 when not specified") {
    val config = createAndGetConfig(baseConfigs)
    config.getProperties.getProperty(DatabaseConfig.SCAN_FETCH_SIZE) shouldBe "4096"
  }

  test("create should not override scan fetch size when already specified") {
    val config = createAndGetConfig(baseConfigs + (DatabaseConfig.SCAN_FETCH_SIZE -> "100"))
    config.getProperties.getProperty(DatabaseConfig.SCAN_FETCH_SIZE) shouldBe "100"
  }

  test("create should force READ_COMMITTED isolation for consensus-commit") {
    val config = createAndGetConfig(
      baseConfigs + (DatabaseConfig.TRANSACTION_MANAGER -> ConsensusCommitConfig.TRANSACTION_MANAGER_NAME)
    )
    config.getProperties.getProperty(
      ConsensusCommitConfig.ISOLATION_LEVEL
    ) shouldBe Isolation.READ_COMMITTED.toString
  }

  test("create should force READ_COMMITTED even when isolation is already set") {
    val config = createAndGetConfig(
      baseConfigs ++ Map(
        DatabaseConfig.TRANSACTION_MANAGER    -> ConsensusCommitConfig.TRANSACTION_MANAGER_NAME,
        ConsensusCommitConfig.ISOLATION_LEVEL -> Isolation.SERIALIZABLE.toString
      )
    )
    config.getProperties.getProperty(
      ConsensusCommitConfig.ISOLATION_LEVEL
    ) shouldBe Isolation.READ_COMMITTED.toString
  }

  test(
    "create should force READ_COMMITTED when transaction manager is not set (default is consensus-commit)"
  ) {
    val config = createAndGetConfig(baseConfigs)
    config.getProperties.getProperty(
      ConsensusCommitConfig.ISOLATION_LEVEL
    ) shouldBe Isolation.READ_COMMITTED.toString
  }

  test("create should force cross_partition_scan.enabled to true") {
    val config = createAndGetConfig(baseConfigs)
    config.getProperties.getProperty(DatabaseConfig.CROSS_PARTITION_SCAN) shouldBe "true"
    config.getProperties.getProperty(DatabaseConfig.CROSS_PARTITION_SCAN_FILTERING) shouldBe "true"
  }

  test("create should force cross_partition_scan.enabled even when set to false") {
    val config = createAndGetConfig(baseConfigs + (DatabaseConfig.CROSS_PARTITION_SCAN -> "false"))
    config.getProperties.getProperty(DatabaseConfig.CROSS_PARTITION_SCAN) shouldBe "true"
    config.getProperties.getProperty(DatabaseConfig.CROSS_PARTITION_SCAN_FILTERING) shouldBe "true"
  }

  test("create should not set isolation level for jdbc transaction manager") {
    val config = createAndGetConfig(
      baseConfigs + (DatabaseConfig.TRANSACTION_MANAGER -> JdbcConfig.TRANSACTION_MANAGER_NAME)
    )
    Option(config.getProperties.getProperty(ConsensusCommitConfig.ISOLATION_LEVEL)) shouldBe None
  }
}
