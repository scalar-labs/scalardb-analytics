/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.databricks

import com.scalar.db.analytics.api.request.RegisterDataSourceRequest
import com.scalar.db.analytics.spark.{BaseSpec, CatalogTestBase}
import com.scalar.db.analytics.spark.util.{CatalogUtil, DateTimeUtil}
import org.apache.spark.sql.catalyst.analysis.NoSuchNamespaceException
import org.apache.spark.sql.connector.catalog.{Column, Identifier}
import org.apache.spark.sql.types.DataTypes
import org.scalatest.BeforeAndAfterAll

import java.time.{LocalDate, LocalDateTime, ZoneId, ZonedDateTime}
import scala.io.Source

/** Tests for Databricks catalog integration using ScalarDB Analytics Server/Client architecture.
  *
  * This test requires Databricks environment variables to be set. The test will be skipped if
  * ANALYTICS_INT_TEST_ENV != "CI".
  *
  * Required environment variables:
  *   - ANALYTICS_INT_TEST_ENV=CI
  *   - ANALYTICS_INT_TEST_ENV_DATABRICKS_HOST
  *   - ANALYTICS_INT_TEST_ENV_DATABRICKS_HTTP_PATH
  *   - ANALYTICS_INT_TEST_ENV_DATABRICKS_O_AUTH_CLIENT_ID
  *   - ANALYTICS_INT_TEST_ENV_DATABRICKS_O_AUTH_SECRET
  *
  * This test verifies:
  *   - Unity Catalog and Hive Metastore support
  *   - 21 different column types including DECIMAL type mappings
  *   - TIMESTAMP_NTZ timezone handling
  */
class CatalogWithDatabricksTest extends BaseSpec with CatalogTestBase with BeforeAndAfterAll {

  override protected val CATALOG_NAME = "test_catalog"

  private lazy val databricksEnv = new DatabricksEnv()
  private lazy val catalog1      = s"spark_embedded_test_catalog1_${databricksEnv.catalogSuffix}"
  private lazy val catalog2      = s"spark_embedded_test_catalog2_${databricksEnv.catalogSuffix}"
  private lazy val schemaHive1 = s"spark_embedded_test_schema_hive1_${databricksEnv.catalogSuffix}"
  private lazy val schemaHive2 = s"spark_embedded_test_schema_hive2_${databricksEnv.catalogSuffix}"

  // Test tables for parameterized tests
  private val testTables = Seq(
    ("databricks", Seq("databricks", catalog1, "schema1"), "test_table"),
    ("databricks", Seq("databricks", "hive_metastore", schemaHive1), "test_table_hive")
  )

  override protected def beforeAll(): Unit = {
    // Check environment variable
    if (!sys.env.get(DatabricksEnv.ANALYTICS_INT_TEST_ENV).contains("CI")) {
      cancel(
        s"${DatabricksEnv.ANALYTICS_INT_TEST_ENV}=CI environment variable is required for Databricks tests"
      )
    }

    // Start server bundle
    startServerBundle()

    // Create catalog
    analyticsClient.catalog().createCatalog(CATALOG_NAME)

    // Execute SQL script to create catalogs/schemas/tables in Databricks
    // This must be done BEFORE registering the data source
    executeSqlScript("sql/init_databricks.sql")

    // Register Databricks data source using Analytics Client API
    // This will recognize the catalogs/schemas created above
    val provider = databricksEnv.databricks
    val request = new RegisterDataSourceRequest(
      CATALOG_NAME,
      "databricks",
      provider,
      null // schema is null for Databricks
    )
    analyticsClient.dataSource().register(request)

    // Initialize Spark session after containers and data sources are ready
    super.beforeAll()
  }

  override protected def afterAll(): Unit =
    try {
      // Clean up Spark session first
      super.afterAll()

      // Stop server bundle
      stopServerBundle()
    } finally {
      // Clean up Databricks resources
      executeSql(
        s"DROP CATALOG IF EXISTS $catalog1 CASCADE",
        s"DROP CATALOG IF EXISTS $catalog2 CASCADE",
        s"DROP SCHEMA IF EXISTS hive_metastore.$schemaHive1 CASCADE",
        s"DROP SCHEMA IF EXISTS hive_metastore.$schemaHive2 CASCADE"
      )
      databricksEnv.dataSource.close()
    }

  private def executeSqlScript(resourcePath: String): Unit = {
    val script = Source.fromResource(resourcePath).mkString
    val replacedScript = script
      .replaceAll("spark_embedded_test_catalog1", catalog1)
      .replaceAll("spark_embedded_test_catalog2", catalog2)
      .replaceAll("spark_embedded_test_schema_hive1", schemaHive1)
      .replaceAll("spark_embedded_test_schema_hive2", schemaHive2)

    val sqls = replacedScript.split(";").map(_.trim).filter(_.nonEmpty)

    executeSql(sqls.toIndexedSeq: _*)
  }

  private def executeSql(sqls: String*): Unit = {
    val connection = databricksEnv.dataSource.getConnection
    try {
      val statement = connection.createStatement()
      try
        sqls.foreach(statement.execute)
      finally
        statement.close()
    } finally
      connection.close()
  }

  describe("Catalog") {
    describe("ListNamespace") {
      it("should list namespaces") {
        val catalog    = CatalogUtil.getCatalog(spark, CATALOG_NAME)
        val namespaces = catalog.listNamespaces().map(_.mkString("."))

        // Verify only the existence of namespaces related to this test to avoid conflicting with
        // other parallel integration tests run
        val ns1 = s"databricks.$catalog1.default"
        val ns2 = s"databricks.$catalog1.schema1"
        val ns3 = s"databricks.$catalog1.schema2"
        val ns4 = s"databricks.$catalog2.default"
        val ns5 = s"databricks.$catalog2.schema1"
        val ns6 = s"databricks.$catalog2.schema2"
        val ns7 = s"databricks.hive_metastore.$schemaHive1"
        val ns8 = s"databricks.hive_metastore.$schemaHive2"

        assert(namespaces.contains("databricks.main.default"))
        assert(namespaces.contains(ns1))
        assert(namespaces.contains(ns2))
        assert(namespaces.contains(ns3))
        assert(namespaces.contains(ns4))
        assert(namespaces.contains(ns5))
        assert(namespaces.contains(ns6))
        assert(namespaces.contains(ns7))
        assert(namespaces.contains(ns8))
      }

      it("should list namespaces with prefix") {
        val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
        val namespaces =
          catalog.listNamespaces(Array("databricks", "main", "default"))

        assert(namespaces.length == 1)
        assert(namespaces(0) sameElements Array("databricks", "main", "default"))
      }

      it("should throw NoSuchNamespaceException when namespace does not exist") {
        val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)

        assertThrows[NoSuchNamespaceException] {
          catalog.listNamespaces(Array("databricks", catalog1, "schema3"))
        }
      }
    }

    describe("ListTables") {
      testTables.foreach { case (_, namespace, tableName) =>
        val fqn = (namespace :+ tableName).mkString(".")

        it(s"should list tables for $fqn") {
          val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
          val tables  = catalog.listTables(namespace.toArray).map(_.toString).toSet

          assert(tables == Set(fqn))
        }
      }

      it("should throw NoSuchNamespaceException when namespace does not exist") {
        val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)

        assertThrows[NoSuchNamespaceException] {
          catalog.listTables(Array("databricks", catalog1, "schema3"))
        }
      }
    }

    describe("LoadTable") {
      testTables.foreach { case (_, namespace, tableName) =>
        val fqn = (namespace :+ tableName).mkString(".")

        describe(s"for $fqn") {
          it("should load table") {
            val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
            val table   = catalog.loadTable(Identifier.of(namespace.toArray, tableName))

            assert(table.name() == fqn)
          }

          it("should have correct columns") {
            val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
            val table   = catalog.loadTable(Identifier.of(namespace.toArray, tableName))
            val columns = table.columns()

            val expected = Array(
              Column.create("tinyint_col", DataTypes.ShortType),
              Column.create("smallint_col", DataTypes.ShortType),
              Column.create("int_col", DataTypes.IntegerType),
              Column.create("bigint_col", DataTypes.LongType),
              Column.create("float_col", DataTypes.FloatType),
              Column.create("double_col", DataTypes.DoubleType),
              Column.create("decimal_20_col", DataTypes.ByteType),
              Column.create("decimal_30_col", DataTypes.ShortType),
              Column.create("decimal_40_col", DataTypes.ShortType),
              Column.create("decimal_50_col", DataTypes.IntegerType),
              Column.create("decimal_90_col", DataTypes.IntegerType),
              Column.create("decimal_100_col", DataTypes.LongType),
              Column.create("decimal_180_col", DataTypes.LongType),
              Column.create("decimal_190_col", DataTypes.createDecimalType(19, 0)),
              Column.create("decimal_380_col", DataTypes.createDecimalType(38, 0)),
              Column.create("string_col", DataTypes.StringType),
              Column.create("binary_col", DataTypes.BinaryType),
              Column.create("boolean_col", DataTypes.BooleanType),
              Column.create("date_col", DataTypes.DateType),
              Column.create("timestamp_col", DataTypes.TimestampType),
              Column.create("timestamp_ntz_col", DataTypes.TimestampNTZType)
            )

            assert(columns sameElements expected)
          }

          it("should read data") {
            val rows = spark.sql(s"SELECT * FROM $CATALOG_NAME.$fqn").collect()

            assert(rows.length == 1)
            val row = rows(0)

            // Numeric types
            assert(row.getShort(0) == 127.toShort)         // tinyint_col
            assert(row.getShort(1) == 32767.toShort)       // smallint_col
            assert(row.getInt(2) == 2147483647)            // int_col
            assert(row.getLong(3) == 9223372036854775807L) // bigint_col
            assert(row.getFloat(4) == 3.402e+38f)          // float_col
            assert(row.getDouble(5) == 1.79769e+308d)      // double_col

            // DECIMAL types with various scales
            assert(row.getByte(6) == 99.toByte)            // decimal_20_col
            assert(row.getShort(7) == 100.toShort)         // decimal_30_col
            assert(row.getShort(8) == 9999.toShort)        // decimal_40_col
            assert(row.getInt(9) == 10000)                 // decimal_50_col
            assert(row.getInt(10) == 999999999)            // decimal_90_col
            assert(row.getLong(11) == 1000000000L)         // decimal_100_col
            assert(row.getLong(12) == 999999999999999999L) // decimal_180_col
            assert(
              row.getDecimal(13).compareTo(BigDecimal(10).pow(18).bigDecimal) == 0
            ) // decimal_190_col
            assert(
              row.getDecimal(14).compareTo(BigDecimal(10).pow(37).bigDecimal) == 0
            ) // decimal_380_col

            // String and binary types
            assert(row.getString(15) == "foo bar baz") // string_col
            assert(
              row.get(16).asInstanceOf[Array[Byte]] sameElements Array[Byte](
                0x1a.toByte,
                0xbf.toByte
              )
            )                          // binary_col
            assert(row.getBoolean(17)) // boolean_col

            // Date and timestamp types
            assert(row.getDate(18).toLocalDate == LocalDate.of(2000, 7, 14)) // date_col

            val localDateTime = LocalDateTime.of(2000, 7, 14, 12, 34, 56, 123000000)
            assert(
              row.getTimestamp(19).toInstant == ZonedDateTime
                .of(localDateTime, ZoneId.systemDefault())
                .toInstant
            ) // timestamp_col

            // TIMESTAMP_NTZ: Spark returns LocalDateTime converted to UTC
            assert(
              row.get(20) == DateTimeUtil.localToUtc(localDateTime.withNano(123456000))
            ) // timestamp_ntz_col
          }
        }
      }
    }
  }
}
