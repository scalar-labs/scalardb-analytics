/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.sqlserver

import com.scalar.db.analytics.api.model.datasource.provider.rdbms.SqlServer
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest
import com.scalar.db.analytics.spark.util.TestContainers.MSSQLServerContainer
import com.scalar.db.analytics.spark.util.{CatalogUtil, DateTimeUtil}
import com.scalar.db.analytics.spark.{BaseSpec, CatalogTestBase}
import org.apache.spark.sql.catalyst.analysis.NoSuchNamespaceException
import org.apache.spark.sql.connector.catalog.{Column, Identifier}
import org.apache.spark.sql.types.DataTypes
import org.scalatest.BeforeAndAfterAll
import org.testcontainers.utility.DockerImageName

import java.time.{LocalDate, LocalDateTime}

trait CatalogWithSqlServerTestBase extends BaseSpec with CatalogTestBase with BeforeAndAfterAll {

  protected def sqlServerImage: DockerImageName

  override protected val CATALOG_NAME = "test_catalog"

  private val DATA_SOURCE_NAME       = "sqlserver"
  private val INIT_SQL_SERVER_SCRIPT = "sql/init_sql_server.sql"

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var sqlServerContainer: Option[MSSQLServerContainer] = None

  override protected def beforeAll(): Unit = {
    // Start an SQL Server container (not on Docker network, uses port mapping)
    val sqlServer = new MSSQLServerContainer(sqlServerImage)
      .withInitScript(INIT_SQL_SERVER_SCRIPT)
      .acceptLicense()
    sqlServer.start()
    sqlServerContainer = Some(sqlServer)

    // Start PostgreSQL and server containers (creates the network)
    startServerBundle()

    // Create catalog
    analyticsClient.catalog().createCatalog(CATALOG_NAME)

    // Register SQL Server data source (using localhost accessible from both server and Spark)
    sqlServerContainer.fold(
      throw new IllegalStateException("SQL Server container not initialized")
    ) { sqlServer =>
      val provider = SqlServer
        .builder()
        .host("localhost") // localhost accessible from both server (via host-gateway) and Spark
        .port(sqlServer.mappedPort(1433)) // Use mapped port on host
        .username(sqlServer.username)
        .password(sqlServer.password)
        .secure(false)
        .build()

      @SuppressWarnings(Array("org.wartremover.warts.Null"))
      val request = new RegisterDataSourceRequest(
        CATALOG_NAME,
        DATA_SOURCE_NAME,
        provider,
        null // schema is optional for SQL Server (supports auto-resolution)
      )

      analyticsClient.dataSource().register(request)
    }

    // Initialize a Spark session after containers and data sources are ready
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    // Stop Spark session first
    super.afterAll()

    // Stop PostgreSQL and server containers
    stopServerBundle()

    // Stop SQL Server container
    sqlServerContainer.foreach(_.stop())
    sqlServerContainer = None
  }

  describe("Catalog") {
    // ListNamespace tests
    it("should list namespaces") {
      val catalog    = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val namespaces = catalog.listNamespaces().map(_.mkString(".")).toSet
      assert(
        namespaces == Set(
          "sqlserver.testdb.dbo",
          "sqlserver.testdb.schema1",
          "sqlserver.testdb.schema2",
          "sqlserver.testdb2.dbo",
          "sqlserver.testdb2.schema1",
          "sqlserver.testdb2.schema2"
        )
      )
    }

    it("should list namespaces with prefix") {
      val catalog    = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val namespaces = catalog.listNamespaces(Array("sqlserver", "testdb", "dbo"))
      assert(namespaces.length == 1)
      assert(namespaces(0) sameElements Array("sqlserver", "testdb", "dbo"))
    }

    it("should throw NoSuchNamespaceException when namespace does not exist (listNamespaces)") {
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      assertThrows[NoSuchNamespaceException] {
        catalog.listNamespaces(Array("sqlserver", "testdb", "schema3"))
      }
    }

    // ListTables tests
    it("should list tables") {
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val tables = catalog.listTables(Array("sqlserver", "testdb", "schema1")).map(_.toString).toSeq
      assert(tables == Seq("sqlserver.testdb.schema1.test_table"))
    }

    it("should throw NoSuchNamespaceException when namespace does not exist (listTables)") {
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      assertThrows[NoSuchNamespaceException] {
        catalog.listTables(Array("sqlserver", "testdb", "schema3"))
      }
    }

    // LoadTable tests
    it("should load table") {
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val table =
        catalog.loadTable(Identifier.of(Array("sqlserver", "testdb", "schema1"), "test_table"))
      assert(table.name() == "sqlserver.testdb.schema1.test_table")
    }

    it("should have correct columns") {
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val table =
        catalog.loadTable(Identifier.of(Array("sqlserver", "testdb", "schema1"), "test_table"))
      val columns = table.columns()
      val expected = Array(
        Column.create("bit_col", DataTypes.BooleanType),
        Column.create("tinyint_col", DataTypes.ShortType),
        Column.create("smallint_col", DataTypes.ShortType),
        Column.create("int_col", DataTypes.IntegerType),
        Column.create("bigint_col", DataTypes.LongType),
        Column.create("real_col", DataTypes.FloatType),
        Column.create("float_col", DataTypes.DoubleType),
        Column.create("float24_col", DataTypes.FloatType),
        Column.create("float25_col", DataTypes.DoubleType),
        Column.create("float53_col", DataTypes.DoubleType),
        Column.create("binary_col", DataTypes.BinaryType),
        Column.create("varbinary_col", DataTypes.BinaryType),
        Column.create("char_col", DataTypes.StringType),
        Column.create("varchar_col", DataTypes.StringType),
        Column.create("nchar_col", DataTypes.StringType),
        Column.create("nvarchar_col", DataTypes.StringType),
        Column.create("text_col", DataTypes.StringType),
        Column.create("ntext_col", DataTypes.StringType),
        Column.create("date_col", DataTypes.DateType),
        Column.create("time_col", DataTypes.TimestampNTZType),
        Column.create("datetime_col", DataTypes.TimestampNTZType),
        Column.create("datetime2_col", DataTypes.TimestampNTZType),
        Column.create("smalldatetime_col", DataTypes.TimestampNTZType),
        Column.create("datetimeoffset_col", DataTypes.TimestampType)
      )
      assert(columns.toSeq == expected.toSeq)
    }

    it("should read data") {
      val rows =
        spark.sql(s"SELECT * FROM $CATALOG_NAME.sqlserver.testdb.schema1.test_table").collect()
      assert(rows.length == 1)

      val row = rows(0)
      assert(row.getBoolean(0))                      // bit_col
      assert(row.getShort(1) == 255.toShort)         // tinyint_col
      assert(row.getShort(2) == 32767.toShort)       // smallint_col
      assert(row.getInt(3) == 2147483647)            // int_col
      assert(row.getLong(4) == 9223372036854775807L) // bigint_col
      assert(row.getFloat(5) == 123.45f)             // real_col
      assert(row.getDouble(6) == 12345.6789)         // float_col
      assert(row.getFloat(7) == 123.45f)             // float24_col
      assert(row.getDouble(8) == 12345.6789)         // float25_col
      assert(row.getDouble(9) == 123456.789)         // float53_col
      val binaryCol = row.get(10).asInstanceOf[Array[Byte]]
      assert(
        binaryCol sameElements Array[Byte](0x12, 0x34, 0x56, 0x78, 0x90.toByte, 0, 0, 0, 0, 0)
      ) // binary_col
      val varbinaryCol = row.get(11).asInstanceOf[Array[Byte]]
      assert(
        varbinaryCol sameElements Array[Byte](0xab.toByte, 0xcd.toByte, 0xef.toByte)
      )                                                    // varbinary_col
      assert(row.getString(12) == "CHAR_TEXT ")            // char_col (padded to 10 chars)
      assert(row.getString(13) == "VARCHAR_TXT")           // varchar_col
      assert(row.getString(14) == "NCHAR_TXT ")            // nchar_col (padded to 10 chars)
      assert(row.getString(15) == "NVARCHAR_TXT")          // nvarchar_col
      assert(row.getString(16) == "Sample text for TEXT")  // text_col
      assert(row.getString(17) == "Sample text for NTEXT") // ntext_col
      assert(row.getDate(18).toLocalDate == LocalDate.parse("2024-10-27")) // date_col
      val timeCol = row.get(19).asInstanceOf[LocalDateTime]
      assert(
        timeCol == DateTimeUtil.localToUtc(parseLocalTime("15:30:00"))
      ) // time_col
      val datetimeCol = row.get(20).asInstanceOf[LocalDateTime]
      assert(
        datetimeCol == DateTimeUtil.localToUtc(LocalDateTime.parse("2024-10-27T15:30:00"))
      ) // datetime_col
      val datetime2Col = row.get(21).asInstanceOf[LocalDateTime]
      assert(
        datetime2Col == DateTimeUtil.localToUtc(LocalDateTime.parse("2024-10-27T15:30:00.123456"))
      ) // datetime2_col
      val smalldatetimeCol = row.get(22).asInstanceOf[LocalDateTime]
      assert(
        smalldatetimeCol == DateTimeUtil.localToUtc(LocalDateTime.parse("2024-10-27T15:30:00"))
      ) // smalldatetime_col
      assert(
        row.getTimestamp(23).toInstant == java.time.OffsetDateTime
          .parse("2024-10-27T15:30:00+00:00")
          .toInstant
      ) // datetimeoffset_col
    }
  }

  private def parseLocalTime(time: String): LocalDateTime =
    LocalDateTime.of(LocalDate.parse("1900-01-01"), java.time.LocalTime.parse(time))
}
