/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.oracle

import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Oracle
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest
import com.scalar.db.analytics.spark.util.TestContainers.GenericContainer
import com.scalar.db.analytics.spark.util.{CatalogUtil, TestImages}
import com.scalar.db.analytics.spark.{BaseSpec, CatalogTestBase}
import org.apache.spark.sql.catalyst.analysis.NoSuchNamespaceException
import org.apache.spark.sql.connector.catalog.{Column, Identifier}
import org.apache.spark.sql.types.DataTypes
import org.scalatest.BeforeAndAfterAll
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

import java.nio.charset.StandardCharsets
import java.time.{Instant, LocalDateTime}

trait CatalogWithOracleTestBase extends BaseSpec with CatalogTestBase with BeforeAndAfterAll {

  protected def oracleImage: DockerImageName
  protected def initScript: String
  protected def oracleServiceName: String
  protected def hasBooleanColumn: Boolean = true // Oracle 23c has BOOLEAN, 19c does not

  override protected val CATALOG_NAME = "test_catalog"

  private val DATA_SOURCE_NAME = "oracle"
  // For ghcr.io/scalar-labs/oracle/db-prebuilt:23, use SYSTEM/Oracle
  // For gvenzl/oracle-*, use SYSTEM/Oracle as well
  private val ORACLE_USERNAME = "SYSTEM"
  private val ORACLE_PASSWORD = "Oracle"

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var oracleContainer: Option[GenericContainer] = None

  private def executeInitScript(oracle: GenericContainer): Unit = {
    // Copy init script to container
    oracle.copyFileToContainer(
      org.testcontainers.utility.MountableFile.forClasspathResource(initScript),
      "/tmp/init.sql"
    )

    // Execute script using sqlplus with NLS_LANG set to UTF8 for proper multibyte character handling
    val result = oracle.execInContainer(
      "sh",
      "-c",
      s"NLS_LANG=JAPANESE_JAPAN.AL32UTF8 sqlplus -S $ORACLE_USERNAME/$ORACLE_PASSWORD@$oracleServiceName @/tmp/init.sql"
    )

    if (result.getExitCode != 0) {
      throw new RuntimeException(
        s"Failed to execute init script. Exit code: ${result.getExitCode.toString}\n" +
          s"Stdout: ${result.getStdout}\n" +
          s"Stderr: ${result.getStderr}"
      )
    }
  }

  override protected def beforeAll(): Unit = {
    // Start an Oracle container using GenericContainer (not on Docker network, uses port mapping)
    // Using GenericContainer allows us to use ghcr.io/scalar-labs/oracle/db-prebuilt images
    // with SYSTEM/Oracle credentials without OracleContainer's user creation logic
    val oracle = new GenericContainer(oracleImage)
      .withExposedPorts(1521)
      .withEnv("ORACLE_PASSWORD", ORACLE_PASSWORD)
      .waitingFor(
        Wait
          .forLogMessage(".*DATABASE IS READY TO USE!.*\\s", 1)
          .withStartupTimeout(TestImages.ORACLE_STARTUP_TIMEOUT)
      )
    oracle.start()
    oracleContainer = Some(oracle)

    // Execute initialization script manually (prebuilt image doesn't have auto-init)
    executeInitScript(oracle)

    // Start a server bundle (creates the network)
    startServerBundle()

    // Create catalog
    analyticsClient.catalog().createCatalog(CATALOG_NAME)

    // Register an Oracle data source (using localhost accessible from both server and Spark)
    oracleContainer.fold(
      throw new IllegalStateException("Oracle container not initialized")
    ) { oracle =>
      val provider = Oracle
        .builder()
        .host("localhost") // localhost accessible from both server (via host-gateway) and Spark
        .port(oracle.mappedPort(1521)) // Use mapped port on host
        .username(ORACLE_USERNAME)
        .password(ORACLE_PASSWORD)
        .serviceName(oracleServiceName)
        .build()

      @SuppressWarnings(Array("org.wartremover.warts.Null"))
      val request = new RegisterDataSourceRequest(
        CATALOG_NAME,
        DATA_SOURCE_NAME,
        provider,
        null
      )

      analyticsClient.dataSource().register(request)
    }

    // Initialize a Spark session after containers and data sources are ready
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    // Stop Spark session first
    super.afterAll()

    // Stop server bundle
    stopServerBundle()

    // Stop Oracle container
    oracleContainer.foreach(_.stop())
    oracleContainer = None
  }

  describe("Catalog") {
    // ListNamespace tests
    it("should list namespaces") {
      val catalog    = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val namespaces = catalog.listNamespaces().map(_.mkString(".")).toSet
      // Oracle prebuilt image includes OLAPSYS system schema
      assert(namespaces.contains("oracle.TESTUSER"))
      assert(namespaces.contains("oracle.OLAPSYS"))
    }

    it("should list namespaces with prefix") {
      val catalog    = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val namespaces = catalog.listNamespaces(Array("oracle", "TESTUSER"))
      assert(namespaces.length == 1)
      assert(namespaces(0) sameElements Array("oracle", "TESTUSER"))
    }

    it("should throw NoSuchNamespaceException when namespace does not exist (listNamespaces)") {
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      assertThrows[NoSuchNamespaceException] {
        catalog.listNamespaces(Array("oracle", "NOT_EXIST"))
      }
    }

    // ListTables tests
    it("should list tables") {
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val tables  = catalog.listTables(Array("oracle", "TESTUSER")).map(_.toString).toSeq
      assert(tables == Seq("oracle.TESTUSER.TEST_TABLE"))
    }

    it("should throw NoSuchNamespaceException when namespace does not exist (listTables)") {
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      assertThrows[NoSuchNamespaceException] {
        catalog.listTables(Array("oracle", "NOT_EXIST"))
      }
    }

    // LoadTable tests
    it("should load table") {
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val table   = catalog.loadTable(Identifier.of(Array("oracle", "TESTUSER"), "TEST_TABLE"))
      assert(table.name() == "oracle.TESTUSER.TEST_TABLE")
    }

    it("should have correct columns") {
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val table   = catalog.loadTable(Identifier.of(Array("oracle", "TESTUSER"), "TEST_TABLE"))
      val columns = table.columns()

      val commonColumns = Array(
        Column.create("NUMBER_COL", DataTypes.DoubleType),
        Column.create("NUMBER_WITHOUT_SCALE_COL", DataTypes.LongType),
        Column.create("NUMBER_WITH_SCALE_COL", DataTypes.DoubleType),
        Column.create("FLOAT_WITH_PRECISION_53_COL", DataTypes.DoubleType),
        Column.create("BINARY_FLOAT_COL", DataTypes.FloatType),
        Column.create("BINARY_DOUBLE_COL", DataTypes.DoubleType),
        Column.create("CHAR_COL", DataTypes.StringType),
        Column.create("NCHAR_COL", DataTypes.StringType),
        Column.create("VARCHAR2_COL", DataTypes.StringType),
        Column.create("NVARCHAR2_COL", DataTypes.StringType),
        Column.create("CLOB_COL", DataTypes.StringType),
        Column.create("NCLOB_COL", DataTypes.StringType),
        Column.create("BLOB_COL", DataTypes.BinaryType),
        Column.create("DATE_COL", DataTypes.DateType),
        Column.create("TIMESTAMP_COL", DataTypes.TimestampType),
        Column.create("TIMESTAMP_WITH_TIME_ZONE_COL", DataTypes.TimestampType),
        Column.create("TIMESTAMP_WITH_LOCAL_TIME_ZONE_COL", DataTypes.TimestampNTZType),
        Column.create("RAW_COL", DataTypes.BinaryType)
      )

      val expected = if (hasBooleanColumn) {
        commonColumns :+ Column.create("BOOLEAN_COL", DataTypes.BooleanType)
      } else {
        commonColumns
      }

      assert(columns.toSeq == expected.toSeq)
    }

    it("should read data") {
      val rows = spark.sql(s"SELECT * FROM $CATALOG_NAME.oracle.TESTUSER.TEST_TABLE").collect()
      assert(rows.length == 1)

      val row = rows(0)
      assert(row.getDouble(0) == 12345.0)          // number_col
      assert(row.getLong(1) == 123456789012345L)   // number_without_scale_col
      assert(row.getDouble(2) == 123.4567890123)   // number_with_scale_col
      assert(row.getDouble(3) == 12345.6789)       // float_with_precision_53_col
      assert(row.getFloat(4) == 1.23f)             // binary_float_col
      assert(row.getDouble(5) == 123.45678)        // binary_double_col
      assert(row.getString(6) == "CHAR_TEXT ")     // char_col (padded to 10 chars)
      assert(row.getString(7) == "NCHARテキスト ")     // nchar_col (padded to 10 chars)
      assert(row.getString(8) == "VCHAR_TEXT")     // varchar2_col
      assert(row.getString(9) == "NVCHARテキスト")     // nvarchar2_col
      assert(row.getString(10) == "CLOBサンプルテキスト")  // clob_col
      assert(row.getString(11) == "NCLOBサンプルテキスト") // nclob_col
      val blobCol = row.get(12).asInstanceOf[Array[Byte]]
      assert(blobCol sameElements "BLOBデータ".getBytes(StandardCharsets.UTF_8))        // blob_col
      assert(row.getDate(13).toLocalDate == java.time.LocalDate.parse("2024-10-27")) // date_col
      assert(
        row.getTimestamp(14).toLocalDateTime == LocalDateTime.parse("2024-10-27T15:30:00")
      ) // timestamp_col
      assert(
        row.getTimestamp(15).toInstant == Instant.parse("2024-10-27T15:30:00Z")
      ) // timestamp_with_time_zone_col
      val timestampWithLocalTimeZoneCol = row.get(16).asInstanceOf[LocalDateTime]
      // TIMESTAMP WITH LOCAL TIME ZONE is returned in session timezone
      // With NLS_LANG=JAPANESE_JAPAN.AL32UTF8, session timezone is JST
      assert(
        timestampWithLocalTimeZoneCol == LocalDateTime.parse("2024-10-27T15:30:00")
      ) // timestamp_with_local_time_zone_col (returned in JST due to NLS_LANG setting)
      val rawCol = row.get(17).asInstanceOf[Array[Byte]]
      assert(rawCol sameElements "RAWデータ".getBytes(StandardCharsets.UTF_8)) // raw_col

      // BOOLEAN column only exists in Oracle 23c (last column)
      if (hasBooleanColumn) {
        assert(row.getBoolean(18)) // boolean_col
      }
    }
  }
}
