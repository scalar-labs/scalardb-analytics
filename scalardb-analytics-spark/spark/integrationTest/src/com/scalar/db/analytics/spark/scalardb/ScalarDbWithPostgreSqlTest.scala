/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.scalardb

import com.scalar.db.analytics.spark.util.TestContainers.PostgreSQLContainer
import com.scalar.db.analytics.spark.util.TestImages
import org.scalatest.BeforeAndAfterAll

class ScalarDbWithPostgreSqlTest extends ScalarDbTestBase with BeforeAndAfterAll {

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var postgresContainer: Option[PostgreSQLContainer] = None

  override protected def beforeAll(): Unit = {
    // Start PostgreSQL container
    val postgres = new PostgreSQLContainer(TestImages.POSTGRESQL_16)
    postgres.start()
    postgresContainer = Some(postgres)

    // Call parent beforeAll to set up Analytics Server and ScalarDB
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()

    // Stop PostgreSQL container
    postgresContainer.foreach(_.stop())
    postgresContainer = None
  }

  override protected def getScalarDbConfigs(): Map[String, String] =
    postgresContainer.fold(
      throw new IllegalStateException("PostgreSQL container not initialized")
    ) { postgres =>
      Map(
        "scalar.db.storage"        -> "jdbc",
        "scalar.db.contact_points" -> postgres.getJdbcUrl,
        "scalar.db.username"       -> postgres.getUsername,
        "scalar.db.password"       -> postgres.getPassword
      )
    }
}
