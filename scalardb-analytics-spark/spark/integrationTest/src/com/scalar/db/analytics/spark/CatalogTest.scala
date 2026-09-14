/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark

import com.scalar.db.analytics.api.model.datasource.provider.rdbms.MySql
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest
import com.scalar.db.analytics.spark.util.TestContainers.MySQLContainer
import com.scalar.db.analytics.spark.util.{CatalogUtil, TestImages}
import org.scalatest.BeforeAndAfterAll

/** Tests for basic catalog operations using ScalarDB Analytics Server/Client architecture.
  *
  * This test validates catalog behavior including:
  *   - Successful catalog creation and data source registration
  *   - Error handling for catalogs without data sources
  *   - Error handling for invalid data source configurations
  *   - Idempotent catalog and data source operations
  */
class CatalogTest extends BaseSpec with CatalogTestBase with BeforeAndAfterAll {

  override protected val CATALOG_NAME = "test_catalog"

  private val INIT_MYSQL_SCRIPT = "sql/init_mysql.sql"

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var mysqlContainer: Option[MySQLContainer] = None

  override protected def beforeAll(): Unit = {
    // Start a MySQL container (not on Docker network, uses port mapping)
    val mysql: MySQLContainer = new MySQLContainer(TestImages.MYSQL_80)
      .withDatabaseName("testdb")
      .withClasspathResourceMapping(
        INIT_MYSQL_SCRIPT,
        "/docker-entrypoint-initdb.d/init.sql",
        org.testcontainers.containers.BindMode.READ_ONLY
      )
    mysql.start()
    mysqlContainer = Some(mysql)

    // Start PostgreSQL and server containers
    startServerBundle()

    // Create a catalog (shared across all tests)
    analyticsClient.catalog().createCatalog(CATALOG_NAME)

    // Note: We intentionally do NOT register data sources here.
    // Each test case will handle data source registration to test different scenarios.

    // Initialize a Spark session after containers are ready
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    // Stop Spark session first
    super.afterAll()

    // Stop server containers
    stopServerBundle()

    // Stop MySQL container
    mysqlContainer.foreach(_.stop())
    mysqlContainer = None
  }

  describe("Catalog") {
    it("should load successfully with valid MySQL data source") {
      val dataSourceName = "mysql_valid"

      // Register MySQL data source with valid configuration
      mysqlContainer.fold(
        throw new IllegalStateException("MySQL container not initialized")
      ) { mysql =>
        val provider = MySql
          .builder()
          .host("localhost")
          .port(mysql.mappedPort(3306))
          .username(mysql.username)
          .password(mysql.password)
          .build()

        val request = new RegisterDataSourceRequest(
          CATALOG_NAME,
          dataSourceName,
          provider,
          null
        )

        analyticsClient.dataSource().register(request)
      }

      // Verify catalog can be used - list namespaces should work
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      assert(catalog != null)
      assert(catalog.listNamespaces() != null)
    }

    it("should work when listing namespaces with no data sources registered yet") {
      // At this point, a catalog exists, but no data sources are registered (for this specific test)
      // Note: First test may have registered a datasource, but we test the catalog itself

      // Catalog should be loadable
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      assert(catalog != null)

      // Listing namespaces should succeed (may return empty or datasources from previous tests)
      assert(catalog.listNamespaces() != null)
    }

    it("should reject invalid data source configuration") {
      val dataSourceName = "mysql_invalid"

      // Attempt to register a data source with an invalid configuration (missing username and password)
      mysqlContainer.fold(
        throw new IllegalStateException("MySQL container not initialized")
      ) { mysql =>
        val provider = MySql
          .builder()
          .host("localhost")
          .port(mysql.mappedPort(3306))
          // Missing username and password
          .build()

        val request = new RegisterDataSourceRequest(
          CATALOG_NAME,
          dataSourceName,
          provider,
          null
        )

        // Should throw exception due to missing required fields or connection failure
        assertThrows[Exception] {
          analyticsClient.dataSource().register(request)
        }
      }
    }

    it("should reject duplicate data source registration") {
      val dataSourceName = "mysql_dup"

      // Register data source first time
      mysqlContainer.fold(
        throw new IllegalStateException("MySQL container not initialized")
      ) { mysql =>
        val provider = MySql
          .builder()
          .host("localhost")
          .port(mysql.mappedPort(3306))
          .username(mysql.username)
          .password(mysql.password)
          .build()

        val request = new RegisterDataSourceRequest(
          CATALOG_NAME,
          dataSourceName,
          provider,
          null
        )

        analyticsClient.dataSource().register(request)
      }

      // Attempt to register the same data source again - should throw exception
      mysqlContainer.foreach { mysql =>
        val provider = MySql
          .builder()
          .host("localhost")
          .port(mysql.mappedPort(3306))
          .username(mysql.username)
          .password(mysql.password)
          .build()

        val request = new RegisterDataSourceRequest(
          CATALOG_NAME,
          dataSourceName,
          provider,
          null
        )

        assertThrows[Exception] {
          analyticsClient.dataSource().register(request)
        }
      }

      // But the catalog should still be usable
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      assert(catalog != null)
      assert(catalog.listNamespaces() != null)
    }
  }
}
