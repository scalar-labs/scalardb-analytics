/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.snowflake

import com.scalar.db.analytics.api.request.RegisterDataSourceRequest
import com.scalar.db.analytics.spark.{BaseSpec, CatalogTestBase}
import com.scalar.db.analytics.spark.util.{CatalogUtil, DateTimeUtil}
import org.apache.spark.sql.catalyst.analysis.NoSuchNamespaceException
import org.apache.spark.sql.connector.catalog.{Column, Identifier}
import org.apache.spark.sql.types.DataTypes
import org.scalatest.BeforeAndAfterAll

import java.time.{LocalDate, LocalDateTime, LocalTime, ZoneId, ZoneOffset}
import scala.io.Source

/** Tests for Snowflake catalog integration using ScalarDB Analytics Server/Client architecture.
  *
  * This test requires Snowflake environment variables to be set. The test will be skipped if
  * ANALYTICS_INT_TEST_ENV != "CI".
  *
  * Required environment variables:
  *   - ANALYTICS_INT_TEST_ENV=CI
  *   - ANALYTICS_INT_TEST_ENV_SNOWFLAKE_ACCOUNT
  *   - ANALYTICS_INT_TEST_ENV_SNOWFLAKE_USERNAME
  *   - ANALYTICS_INT_TEST_ENV_SNOWFLAKE_PASSWORD
  *
  * This test verifies:
  *   - 2 databases with PUBLIC and custom schemas
  *   - 20 different column types including DECIMAL type mappings
  *   - TIME, TIMESTAMP_NTZ, TIMESTAMP_LTZ, TIMESTAMP_TZ handling
  *   - HYBRID TABLE support with PRIMARY KEY constraints
  */
class CatalogWithSnowflakeTest extends BaseSpec with CatalogTestBase with BeforeAndAfterAll {

  override protected val CATALOG_NAME = "test_catalog"

  private lazy val snowflakeEnv = new SnowflakeEnv()
  // Snowflake converts unquoted identifiers to uppercase
  private lazy val database1 =
    s"SPARK_EMBEDDED_TEST_CATALOG1_${snowflakeEnv.databaseSuffix}".toUpperCase
  private lazy val database2 =
    s"SPARK_EMBEDDED_TEST_CATALOG2_${snowflakeEnv.databaseSuffix}".toUpperCase

  // Test tables for parameterized tests
  private val testTables = Seq(
    ("snowflake", Seq("snowflake", database1, "SCHEMA1"), "TEST_TABLE"),
    ("snowflake", Seq("snowflake", database1, "SCHEMA1"), "TEST_TABLE_HYBRID")
  )

  override protected def beforeAll(): Unit = {
    // Check environment variable
    if (!sys.env.get(SnowflakeEnv.ANALYTICS_INT_TEST_ENV).contains("CI")) {
      cancel(
        s"${SnowflakeEnv.ANALYTICS_INT_TEST_ENV}=CI environment variable is required for Snowflake tests"
      )
    }

    // Start server bundle
    startServerBundle()

    // Create catalog
    analyticsClient.catalog().createCatalog(CATALOG_NAME)

    // Execute SQL script to create databases/schemas/tables in Snowflake
    // This must be done BEFORE registering the data source
    executeSqlScript("sql/init_snowflake.sql")

    // Register Snowflake data source using Analytics Client API
    // This will recognize the databases/schemas created above
    val provider = snowflakeEnv.snowflake
    val request = new RegisterDataSourceRequest(
      CATALOG_NAME,
      "snowflake",
      provider,
      null // schema is null for Snowflake
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
      // Clean up Snowflake resources
      executeSql(
        s"DROP DATABASE IF EXISTS $database1 CASCADE",
        s"DROP DATABASE IF EXISTS $database2 CASCADE"
      )
      snowflakeEnv.dataSource.close()
    }

  private def executeSqlScript(resourcePath: String): Unit = {
    val script = Source.fromResource(resourcePath).mkString
    val replacedScript = script
      .replaceAll("spark_embedded_test_catalog1", database1)
      .replaceAll("spark_embedded_test_catalog2", database2)

    val sqls = replacedScript.split(";").map(_.trim).filter(_.nonEmpty)

    executeSql(sqls.toIndexedSeq: _*)
  }

  private def executeSql(sqls: String*): Unit = {
    val connection = snowflakeEnv.dataSource.getConnection
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
        val namespaces = catalog.listNamespaces().map(_.mkString(".")).toSet

        // Verify only the existence of namespaces related to this test to avoid conflicting with
        // other parallel integration tests run
        val ns1 = s"snowflake.$database1.PUBLIC"
        val ns2 = s"snowflake.$database1.SCHEMA1"
        val ns3 = s"snowflake.$database1.SCHEMA2"
        val ns4 = s"snowflake.$database2.PUBLIC"
        val ns5 = s"snowflake.$database2.SCHEMA1"
        val ns6 = s"snowflake.$database2.SCHEMA2"

        assert(namespaces.contains(ns1))
        assert(namespaces.contains(ns2))
        assert(namespaces.contains(ns3))
        assert(namespaces.contains(ns4))
        assert(namespaces.contains(ns5))
        assert(namespaces.contains(ns6))
      }

      it("should list namespaces with prefix") {
        val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
        val namespaces =
          catalog.listNamespaces(Array("snowflake", database1, "PUBLIC"))

        assert(namespaces.length == 1)
        assert(namespaces(0) sameElements Array("snowflake", database1, "PUBLIC"))
      }

      it("should throw NoSuchNamespaceException when namespace does not exist") {
        val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)

        assertThrows[NoSuchNamespaceException] {
          catalog.listNamespaces(Array("snowflake", database1, "SCHEMA3"))
        }
      }
    }

    describe("ListTables") {
      it("should list tables") {
        val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
        val tables =
          catalog.listTables(Array("snowflake", database1, "SCHEMA1")).map(_.toString).toSet

        val fqn1 = s"snowflake.$database1.SCHEMA1.TEST_TABLE"
        val fqn2 = s"snowflake.$database1.SCHEMA1.TEST_TABLE_HYBRID"

        assert(tables == Set(fqn1, fqn2))
      }

      it("should throw NoSuchNamespaceException when namespace does not exist") {
        val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)

        assertThrows[NoSuchNamespaceException] {
          catalog.listTables(Array("snowflake", database1, "SCHEMA3"))
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

            // For the hybrid table, the "decimal_20_col" is not nullable since it is a primary key
            val isDecimal20ColNullable = tableName != "TEST_TABLE_HYBRID"

            val expected = Array(
              Column.create("DECIMAL_20_COL", DataTypes.ByteType, isDecimal20ColNullable),
              Column.create("DECIMAL_30_COL", DataTypes.ShortType),
              Column.create("DECIMAL_40_COL", DataTypes.ShortType),
              Column.create("DECIMAL_50_COL", DataTypes.IntegerType),
              Column.create("DECIMAL_90_COL", DataTypes.IntegerType),
              Column.create("DECIMAL_100_COL", DataTypes.LongType),
              Column.create("DECIMAL_180_COL", DataTypes.LongType),
              Column.create("DECIMAL_190_COL", DataTypes.createDecimalType(19, 0)),
              Column.create("DECIMAL_380_COL", DataTypes.createDecimalType(38, 0)),
              Column.create("INT_COL", DataTypes.createDecimalType(38, 0)),
              Column.create("FLOAT_COL", DataTypes.DoubleType),
              Column.create("VARCHAR_COL", DataTypes.StringType),
              Column.create("CHAR_COL", DataTypes.StringType),
              Column.create("BINARY_COL", DataTypes.BinaryType),
              Column.create("BOOLEAN_COL", DataTypes.BooleanType),
              Column.create("DATE_COL", DataTypes.DateType),
              Column.create("TIME_COL", DataTypes.TimestampNTZType),
              Column.create("TIMESTAMP_NTZ_COL", DataTypes.TimestampNTZType),
              Column.create("TIMESTAMP_LTZ_COL", DataTypes.TimestampType),
              Column.create("TIMESTAMP_TZ_COL", DataTypes.TimestampType)
            )

            assert(columns sameElements expected)
          }

          it("should read data") {
            val rows = spark.sql(s"SELECT * FROM $CATALOG_NAME.$fqn").collect()

            assert(rows.length == 1)
            val row = rows(0)

            // DECIMAL types with various scales
            assert(row.getByte(0) == 99.toByte)           // DECIMAL_20_COL
            assert(row.getShort(1) == 100.toShort)        // DECIMAL_30_COL
            assert(row.getShort(2) == 9999.toShort)       // DECIMAL_40_COL
            assert(row.getInt(3) == 10000)                // DECIMAL_50_COL
            assert(row.getInt(4) == 999999999)            // DECIMAL_90_COL
            assert(row.getLong(5) == 1000000000L)         // DECIMAL_100_COL
            assert(row.getLong(6) == 999999999999999999L) // DECIMAL_180_COL
            assert(
              row.getDecimal(7).compareTo(BigDecimal(10).pow(18).bigDecimal) == 0
            ) // DECIMAL_190_COL
            assert(
              row.getDecimal(8).compareTo(BigDecimal(10).pow(37).bigDecimal) == 0
            ) // DECIMAL_380_COL
            assert(
              row.getDecimal(9).compareTo(BigDecimal(10).pow(36).bigDecimal) == 0
            ) // INT_COL

            // FLOAT, String and binary types
            assert(row.getDouble(10) == 3.13d)         // FLOAT_COL
            assert(row.getString(11) == "foo bar baz") // VARCHAR_COL
            assert(row.getString(12) == "c")           // CHAR_COL
            assert(
              row.get(13).asInstanceOf[Array[Byte]] sameElements Array[Byte](
                0x1a.toByte,
                0xbf.toByte
              )
            )                          // BINARY_COL
            assert(row.getBoolean(14)) // BOOLEAN_COL

            // Date type
            assert(row.getDate(15).toLocalDate == LocalDate.of(2000, 7, 14)) // DATE_COL

            // Time and timestamp types
            // Snowflake supports microsecond precision (6 digits), not nanosecond (9 digits)
            val localDateTime = LocalDateTime.of(2000, 7, 14, 12, 34, 56, 123456000)
            val time          = LocalTime.of(12, 34, 56, 123456000)

            // TIME_COL: Extract time part from LocalDateTime
            assert(
              row.get(16).asInstanceOf[LocalDateTime].toLocalTime == time
            )

            // TIMESTAMP_NTZ_COL: Spark returns LocalDateTime converted to UTC
            assert(
              row.get(17) == DateTimeUtil.localToUtc(localDateTime)
            )

            // TIMESTAMP_LTZ_COL: Timezone conversion (Asia/Hong_Kong)
            assert(
              row.getTimestamp(18).toInstant == localDateTime
                .atZone(ZoneId.of("Asia/Hong_Kong"))
                .toInstant
            )

            // TIMESTAMP_TZ_COL: Offset conversion (+07:00)
            assert(
              row.getTimestamp(19).toInstant == localDateTime
                .atOffset(ZoneOffset.ofHours(7))
                .toInstant
            )
          }
        }
      }
    }
  }
}
