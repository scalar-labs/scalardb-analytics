/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.postgresql

import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest
import com.scalar.db.analytics.spark.util.TestContainers.PostgreSQLContainer
import com.scalar.db.analytics.spark.util.{CatalogUtil, DateTimeUtil}
import com.scalar.db.analytics.spark.{BaseSpec, CatalogTestBase}
import org.apache.spark.sql.catalyst.analysis.NoSuchNamespaceException
import org.apache.spark.sql.connector.catalog.{Column, Identifier}
import org.apache.spark.sql.types.DataTypes
import org.scalatest.BeforeAndAfterAll
import org.testcontainers.utility.DockerImageName

import java.time.{LocalDate, LocalDateTime, OffsetDateTime, OffsetTime, ZoneId}

trait CatalogWithPostgreSqlTestBase extends BaseSpec with CatalogTestBase with BeforeAndAfterAll {

  protected def postgresqlImage: DockerImageName

  override protected val CATALOG_NAME = "test_catalog"

  private val DATA_SOURCE_NAME       = "postgresql"
  private val INIT_POSTGRESQL_SCRIPT = "sql/init_postgresql.sql"

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var postgresqlContainer: Option[PostgreSQLContainer] = None

  override protected def beforeAll(): Unit = {
    // Start a PostgreSQL container (not on Docker network, uses port mapping)
    val postgresql = new PostgreSQLContainer(postgresqlImage)
    postgresql
      .withUsername("test")
      .withPassword("test")
      .withClasspathResourceMapping(
        INIT_POSTGRESQL_SCRIPT,
        "/docker-entrypoint-initdb.d/init.sql",
        org.testcontainers.containers.BindMode.READ_ONLY
      )
    postgresql.start()
    postgresqlContainer = Some(postgresql)

    // Start PostgreSQL and server containers (creates the network)
    startServerBundle()

    // Create catalog
    analyticsClient.catalog().createCatalog(CATALOG_NAME)

    // Register a PostgreSQL data source (using localhost accessible from both server and Spark)
    postgresqlContainer.fold(
      throw new IllegalStateException("PostgreSQL container not initialized")
    ) { postgresql =>
      val provider = PostgreSql
        .builder()
        .host("localhost") // localhost accessible from both server (via host-gateway) and Spark
        .port(postgresql.mappedPort(5432)) // Use mapped port on host
        .username(postgresql.username)
        .password(postgresql.password)
        .database(postgresql.databaseName)
        .build()

      @SuppressWarnings(Array("org.wartremover.warts.Null"))
      val request = new RegisterDataSourceRequest(
        CATALOG_NAME,
        DATA_SOURCE_NAME,
        provider,
        null // schema is optional for PostgreSQL (supports auto-resolution)
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

    // Stop PostgreSQL container
    postgresqlContainer.foreach(_.stop())
    postgresqlContainer = None
  }

  describe("Catalog") {
    // ListNamespace tests
    it("should list namespaces") {
      val catalog    = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val namespaces = catalog.listNamespaces().map(_.mkString(".")).toSet
      assert(namespaces == Set("postgresql.public", "postgresql.test", "postgresql.test2"))
    }

    it("should list namespaces with prefix") {
      val catalog    = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val namespaces = catalog.listNamespaces(Array("postgresql", "test"))
      assert(namespaces.length == 1)
      assert(namespaces(0) sameElements Array("postgresql", "test"))
    }

    it("should throw NoSuchNamespaceException when namespace does not exist (listNamespaces)") {
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      assertThrows[NoSuchNamespaceException] {
        catalog.listNamespaces(Array("postgresql", "test3"))
      }
    }

    // ListTables tests
    it("should list tables") {
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val tables  = catalog.listTables(Array("postgresql", "test")).map(_.toString).toSeq
      assert(tables == Seq("postgresql.test.test_table"))
    }

    it("should throw NoSuchNamespaceException when namespace does not exist (listTables)") {
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      assertThrows[NoSuchNamespaceException] {
        catalog.listTables(Array("postgresql", "test3"))
      }
    }

    // LoadTable tests
    it("should load table") {
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val table   = catalog.loadTable(Identifier.of(Array("postgresql", "test"), "test_table"))
      assert(table.name() == "postgresql.test.test_table")
    }

    it("should have correct columns") {
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val table   = catalog.loadTable(Identifier.of(Array("postgresql", "test"), "test_table"))
      val columns = table.columns()
      val expected = Array(
        Column.create("smallint_col", DataTypes.ShortType),
        Column.create("integer_col", DataTypes.IntegerType),
        Column.create("bigint_col", DataTypes.LongType),
        Column.create("real_col", DataTypes.FloatType),
        Column.create("double_col", DataTypes.DoubleType),
        Column.create("smallserial_col", DataTypes.ShortType, false),
        Column.create("serial_col", DataTypes.IntegerType, false),
        Column.create("bigserial_col", DataTypes.LongType, false),
        Column.create("char_col", DataTypes.StringType),
        Column.create("varchar_col", DataTypes.StringType),
        Column.create("text_col", DataTypes.StringType),
        Column.create("bpchar_col", DataTypes.StringType),
        Column.create("boolean_col", DataTypes.BooleanType),
        Column.create("bytea_col", DataTypes.BinaryType),
        Column.create("date_col", DataTypes.DateType),
        Column.create("time_col", DataTypes.TimestampNTZType),
        Column.create("time_with_timezone_col", DataTypes.TimestampNTZType),
        Column.create("time_without_timezone_col", DataTypes.TimestampNTZType),
        Column.create("timestamp_col", DataTypes.TimestampNTZType),
        Column.create("timestamp_with_timezone_col", DataTypes.TimestampType),
        Column.create("timestamp_without_timezone_col", DataTypes.TimestampNTZType)
      )
      assert(columns.toSeq == expected.toSeq)
    }

    it("should read data") {
      val rows = spark.sql(s"SELECT * FROM $CATALOG_NAME.postgresql.test.test_table").collect()
      assert(rows.length == 1)

      val row = rows(0)
      assert(row.getShort(0) == 32767.toShort)            // smallint_col
      assert(row.getInt(1) == 2147483647)                 // integer_col
      assert(row.getLong(2) == 9223372036854775807L)      // bigint_col
      assert(row.getFloat(3) == 123.45f)                  // real_col
      assert(row.getDouble(4) == 12345.6789)              // double_col
      assert(row.getShort(5) == 1.toShort)                // smallserial_col
      assert(row.getInt(6) == 1)                          // serial_col
      assert(row.getLong(7) == 1L)                        // bigserial_col
      assert(row.getString(8) == "CHAR_TEXT ")            // char_col (padded to 10 chars)
      assert(row.getString(9) == "VARCHAR_TXT")           // varchar_col
      assert(row.getString(10) == "Sample text for TEXT") // text_col
      assert(row.getString(11) == "BPCHAR_TXT")           // bpchar_col
      assert(row.getBoolean(12))                          // boolean_col
      val byteaCol = row.get(13).asInstanceOf[Array[Byte]]
      assert(byteaCol sameElements Array[Byte](0x48, 0x65, 0x6c, 0x6c, 0x6f)) // bytea_col ('Hello')
      assert(row.getDate(14).toLocalDate == LocalDate.parse("2024-10-27"))    // date_col
      // Spark 3.4 vs 3.5 behavior difference for TIME and TIMESTAMP types:
      //
      // Spark 3.4:
      //   - TIME and TIMESTAMP WITHOUT TIMEZONE types are converted from system timezone (JST) to UTC
      //   - Example: 15:30:00 JST -> 06:30:00 UTC (9 hours difference)
      //   - This affects: TIME, TIME WITHOUT TIMEZONE, TIMESTAMP, TIMESTAMP WITHOUT TIMEZONE
      //
      // Spark 3.5:
      //   - TIME and TIMESTAMP WITHOUT TIMEZONE types are NOT converted (kept as-is)
      //   - Example: 15:30:00 JST -> 15:30:00 (no conversion)
      //
      // Note: TIMESTAMP WITH TIMEZONE is always converted to UTC in both versions
      val (
        expectedTimeCol,
        expectedTimeWithTimezoneCol,
        expectedTimeWithoutTimezoneCol,
        expectedTimestampCol,
        expectedTimestampWithoutTimezoneCol
      ) = if (org.apache.spark.SPARK_VERSION.startsWith("3.4")) {
        // Spark 3.4: Convert to UTC
        (
          DateTimeUtil.localToUtc(DateTimeUtil.parseLocalTime("15:30:00")),
          DateTimeUtil.localToUtc(parseOffsetTime("15:30:00+00:00")),
          DateTimeUtil.localToUtc(DateTimeUtil.parseLocalTime("15:30:00")),
          DateTimeUtil.localToUtc(LocalDateTime.parse("2024-10-27T15:30:00")),
          DateTimeUtil.localToUtc(LocalDateTime.parse("2024-10-27T15:30:00"))
        )
      } else {
        // Spark 3.5: Keep as-is
        (
          DateTimeUtil.parseLocalTime("15:30:00"),
          parseOffsetTime("15:30:00+00:00"),
          DateTimeUtil.parseLocalTime("15:30:00"),
          LocalDateTime.parse("2024-10-27T15:30:00"),
          LocalDateTime.parse("2024-10-27T15:30:00")
        )
      }

      // Assertions
      val timeCol = row.get(15).asInstanceOf[LocalDateTime]
      assert(timeCol == expectedTimeCol) // time_col
      val timeWithTimezoneCol = row.get(16).asInstanceOf[LocalDateTime]
      assert(timeWithTimezoneCol == expectedTimeWithTimezoneCol) // time_with_timezone_col
      val timeWithoutTimezoneCol = row.get(17).asInstanceOf[LocalDateTime]
      assert(timeWithoutTimezoneCol == expectedTimeWithoutTimezoneCol) // time_without_timezone_col
      val timestampCol = row.get(18).asInstanceOf[LocalDateTime]
      assert(timestampCol == expectedTimestampCol) // timestamp_col
      assert(
        row.getTimestamp(19).toInstant == OffsetDateTime
          .parse("2024-10-27T15:30:00+00:00")
          .toInstant
      ) // timestamp_with_timezone_col
      val timestampWithoutTimezoneCol = row.get(20).asInstanceOf[LocalDateTime]
      assert(
        timestampWithoutTimezoneCol == expectedTimestampWithoutTimezoneCol
      ) // timestamp_without_timezone_col
    }
  }

  private def parseOffsetTime(time: String): LocalDateTime = {
    val offsetTime = OffsetTime.parse(time)
    offsetTime
      .atDate(LocalDate.ofEpochDay(0))
      .atZoneSameInstant(ZoneId.systemDefault())
      .toLocalDateTime
  }
}
