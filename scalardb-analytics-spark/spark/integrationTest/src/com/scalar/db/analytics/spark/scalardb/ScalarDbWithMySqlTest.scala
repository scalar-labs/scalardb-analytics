/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.scalardb

import com.scalar.db.analytics.spark.util.TestContainers.MySQLContainer
import com.scalar.db.analytics.spark.util.TestImages
import org.scalatest.BeforeAndAfterAll

class ScalarDbWithMySqlTest extends ScalarDbTestBase with BeforeAndAfterAll {

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var mysqlContainer: Option[MySQLContainer] = None

  override protected def beforeAll(): Unit = {
    // Start MySQL container
    val mysql = new MySQLContainer(TestImages.MYSQL_80).withUsername("root")
    mysql.start()
    mysqlContainer = Some(mysql)

    // Call parent beforeAll to set up Analytics Server and ScalarDB
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()

    // Stop MySQL container
    mysqlContainer.foreach(_.stop())
    mysqlContainer = None
  }

  override protected def getScalarDbConfigs(): Map[String, String] =
    mysqlContainer.fold(
      throw new IllegalStateException("MySQL container not initialized")
    ) { mysql =>
      Map(
        "scalar.db.storage"        -> "jdbc",
        "scalar.db.contact_points" -> mysql.getJdbcUrl,
        "scalar.db.username"       -> mysql.getUsername,
        "scalar.db.password"       -> mysql.getPassword
      )
    }
}
