/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.scalardb

import com.scalar.db.analytics.spark.util.TestContainers.GenericContainer
import com.scalar.db.analytics.spark.util.TestImages
import org.scalatest.BeforeAndAfterAll
import org.testcontainers.containers.wait.strategy.Wait

class ScalarDbWithOracleTest extends ScalarDbTestBase with BeforeAndAfterAll {

  private val ORACLE_USERNAME     = "SYSTEM"
  private val ORACLE_PASSWORD     = "Oracle"
  private val ORACLE_SERVICE_NAME = "FREEPDB1"

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var oracleContainer: Option[GenericContainer] = None

  override protected def beforeAll(): Unit = {
    // Start Oracle container using GenericContainer
    val oracle = new GenericContainer(TestImages.ORACLE_23)
      .withExposedPorts(1521)
      .withEnv("ORACLE_PASSWORD", ORACLE_PASSWORD)
      .waitingFor(
        Wait
          .forLogMessage(".*DATABASE IS READY TO USE!.*\\s", 1)
          .withStartupTimeout(TestImages.ORACLE_STARTUP_TIMEOUT)
      )
    oracle.start()
    oracleContainer = Some(oracle)

    // Call parent beforeAll to set up Analytics Server and ScalarDB
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()

    // Stop Oracle container
    oracleContainer.foreach(_.stop())
    oracleContainer = None
  }

  override protected def getScalarDbConfigs(): Map[String, String] =
    oracleContainer.fold(
      throw new IllegalStateException("Oracle container not initialized")
    ) { oracle =>
      Map(
        "scalar.db.storage" -> "jdbc",
        "scalar.db.contact_points" -> s"jdbc:oracle:thin:@localhost:${oracle.mappedPort(1521).toString}/$ORACLE_SERVICE_NAME",
        "scalar.db.username" -> ORACLE_USERNAME,
        "scalar.db.password" -> ORACLE_PASSWORD
      )
    }
}
