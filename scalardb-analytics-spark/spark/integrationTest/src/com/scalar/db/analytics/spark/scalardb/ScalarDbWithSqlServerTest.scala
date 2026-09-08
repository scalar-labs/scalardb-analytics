/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.scalardb

import com.scalar.db.analytics.spark.util.TestContainers.MSSQLServerContainer
import com.scalar.db.analytics.spark.util.TestImages
import org.scalatest.BeforeAndAfterAll

class ScalarDbWithSqlServerTest extends ScalarDbTestBase with BeforeAndAfterAll {

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var sqlServerContainer: Option[MSSQLServerContainer] = None

  override protected def beforeAll(): Unit = {
    // Start SQL Server container
    val sqlServer = new MSSQLServerContainer(TestImages.SQL_SERVER_2019).acceptLicense()
    sqlServer.start()
    sqlServerContainer = Some(sqlServer)

    // Call parent beforeAll to set up Analytics Server and ScalarDB
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()

    // Stop SQL Server container
    sqlServerContainer.foreach(_.stop())
    sqlServerContainer = None
  }

  override protected def getScalarDbConfigs(): Map[String, String] =
    sqlServerContainer.fold(
      throw new IllegalStateException("SQL Server container not initialized")
    ) { sqlServer =>
      Map(
        "scalar.db.storage"        -> "jdbc",
        "scalar.db.contact_points" -> sqlServer.getJdbcUrl,
        "scalar.db.username"       -> sqlServer.getUsername,
        "scalar.db.password"       -> sqlServer.getPassword
      )
    }
}
