/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.mysql

import com.scalar.db.analytics.api.model.datasource.provider.rdbms.MySql
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest
import com.scalar.db.analytics.spark.util.TestContainers.MySQLContainer
import com.scalar.db.analytics.spark.util.{CatalogUtil, DateTimeUtil}
import com.scalar.db.analytics.spark.{BaseSpec, CatalogTestBase}
import org.apache.spark.sql.catalyst.analysis.NoSuchNamespaceException
import org.apache.spark.sql.connector.catalog.{Column, Identifier}
import org.apache.spark.sql.types.DataTypes
import org.scalatest.BeforeAndAfterAll
import org.testcontainers.utility.DockerImageName

import java.nio.charset.StandardCharsets
import java.time.{LocalDate, LocalDateTime}

trait CatalogWithMySqlTestBase extends BaseSpec with CatalogTestBase with BeforeAndAfterAll {

  protected def mysqlImage: DockerImageName

  override protected val CATALOG_NAME = "test_catalog"

  private val DATA_SOURCE_NAME  = "mysql"
  private val INIT_MYSQL_SCRIPT = "sql/init_mysql.sql"

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var mysqlContainer: Option[MySQLContainer] = None

  override protected def beforeAll(): Unit = {
    // Start a MySQL container (not on Docker network, uses port mapping)
    val mysql: MySQLContainer = new MySQLContainer(mysqlImage)
      .withDatabaseName("testdb")
      .withClasspathResourceMapping(
        INIT_MYSQL_SCRIPT,
        "/docker-entrypoint-initdb.d/init.sql",
        org.testcontainers.containers.BindMode.READ_ONLY
      )
    mysql.start()
    mysqlContainer = Some(mysql)

    // Start PostgreSQL and server containers (creates the network)
    // Pass MySQL mapped port to server for extraHosts configuration
    startServerBundle()

    // Create catalog
    analyticsClient.catalog().createCatalog(CATALOG_NAME)

    // Register MySQL data source (using localhost accessible from both server and Spark)
    // Note: Not specifying database() to access all databases on the MySQL server
    mysqlContainer.fold(
      throw new IllegalStateException("MySQL container not initialized")
    ) { mysql =>
      val provider = MySql
        .builder()
        .host("localhost") // localhost accessible from both server (via host-gateway) and Spark
        .port(mysql.mappedPort(3306)) // Use mapped port on host
        .username(mysql.username)
        .password(mysql.password)
        // Don't set a database to access all databases
        .build()

      @SuppressWarnings(Array("org.wartremover.warts.Null"))
      val request = new RegisterDataSourceRequest(
        CATALOG_NAME,
        DATA_SOURCE_NAME,
        provider,
        null // schema is optional for MySQL (supports auto-resolution)
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

    // Stop MySQL container
    mysqlContainer.foreach(_.stop())
    mysqlContainer = None
  }

  describe("Catalog") {
    // ListNamespace tests
    it("should list namespaces") {
      val catalog    = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val namespaces = catalog.listNamespaces().map(_.mkString(".")).toSet
      assert(namespaces == Set("mysql.testdb", "mysql.testdb2"))
    }

    it("should list namespaces with prefix") {
      val catalog    = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val namespaces = catalog.listNamespaces(Array("mysql", "testdb"))
      assert(namespaces.length == 1)
      assert(namespaces(0) sameElements Array("mysql", "testdb"))
    }

    it("should throw NoSuchNamespaceException when namespace does not exist (listNamespaces)") {
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      assertThrows[NoSuchNamespaceException] {
        catalog.listNamespaces(Array("mysql", "testdb3"))
      }
    }

    // ListTables tests
    it("should list tables") {
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val tables  = catalog.listTables(Array("mysql", "testdb")).map(_.toString).toSeq
      assert(tables == Seq("mysql.testdb.test_table"))
    }

    it("should throw NoSuchNamespaceException when namespace does not exist (listTables)") {
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      assertThrows[NoSuchNamespaceException] {
        catalog.listTables(Array("mysql", "testdb3"))
      }
    }

    // LoadTable tests
    it("should load table") {
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val table   = catalog.loadTable(Identifier.of(Array("mysql", "testdb"), "test_table"))
      assert(table.name() == "mysql.testdb.test_table")
    }

    it("should have correct columns") {
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val table   = catalog.loadTable(Identifier.of(Array("mysql", "testdb"), "test_table"))
      val columns = table.columns()
      val expected = Array(
        Column.create("bit_col", DataTypes.BooleanType),
        Column.create("bit1_col", DataTypes.BooleanType),
        Column.create("bit2_col", DataTypes.BinaryType),
        Column.create("tinyint_col", DataTypes.ShortType),
        Column.create("tinyint1_col", DataTypes.BooleanType),
        Column.create("boolean_col", DataTypes.BooleanType),
        Column.create("smallint_col", DataTypes.ShortType),
        Column.create("smallint_unsigned_col", DataTypes.IntegerType),
        Column.create("mediumint_col", DataTypes.IntegerType),
        Column.create("mediumint_unsigned_col", DataTypes.IntegerType),
        Column.create("int_col", DataTypes.IntegerType),
        Column.create("int_unsigned_col", DataTypes.LongType),
        Column.create("bigint_col", DataTypes.LongType),
        Column.create("float_col", DataTypes.FloatType),
        Column.create("double_col", DataTypes.DoubleType),
        Column.create("real_col", DataTypes.DoubleType),
        Column.create("char_col", DataTypes.StringType),
        Column.create("varchar_col", DataTypes.StringType),
        Column.create("tinytext_col", DataTypes.StringType),
        Column.create("text_col", DataTypes.StringType),
        Column.create("mediumtext_col", DataTypes.StringType),
        Column.create("longtext_col", DataTypes.StringType),
        Column.create("binary_col", DataTypes.BinaryType),
        Column.create("varbinary_col", DataTypes.BinaryType),
        Column.create("tinyblob_col", DataTypes.BinaryType),
        Column.create("blob_col", DataTypes.BinaryType),
        Column.create("mediumblob_col", DataTypes.BinaryType),
        Column.create("longblob_col", DataTypes.BinaryType),
        Column.create("date_col", DataTypes.DateType),
        Column.create("time_col", DataTypes.TimestampNTZType),
        Column.create("datetime_col", DataTypes.TimestampNTZType),
        Column.create("timestamp_col", DataTypes.TimestampType)
      )
      assert(columns.toSeq == expected.toSeq)
    }

    it("should read data") {
      val rows = spark.sql(s"SELECT * FROM $CATALOG_NAME.mysql.testdb.test_table").collect()
      assert(rows.length == 1)

      val row = rows(0)
      assert(row.getBoolean(0)) // bit_col
      assert(row.getBoolean(1)) // bit1_col
      val bit2Col = row.get(2).asInstanceOf[Array[Byte]]
      assert(bit2Col sameElements Array[Byte](2))          // bit2_col
      assert(row.getShort(3) == 127.toShort)               // tinyint_col
      assert(row.getBoolean(4))                            // tinyint1_col
      assert(row.getBoolean(5))                            // boolean_col
      assert(row.getShort(6) == 32767.toShort)             // smallint_col
      assert(row.getInt(7) == 65535)                       // smallint_unsigned_col
      assert(row.getInt(8) == 8388607)                     // mediumint_col
      assert(row.getInt(9) == 16777215)                    // mediumint_unsigned_col
      assert(row.getInt(10) == 2147483647)                 // int_col
      assert(row.getLong(11) == 4294967295L)               // int_unsigned_col
      assert(row.getLong(12) == 9223372036854775807L)      // bigint_col
      assert(row.getFloat(13) == 4.14f)                    // float_col
      assert(row.getDouble(14) == 4.14159265358979)        // double_col
      assert(row.getDouble(15) == 4.14159)                 // real_col
      assert(row.getString(16) == "char_test")             // char_col
      assert(row.getString(17) == "varchar_test")          // varchar_col
      assert(row.getString(18) == "tiny text data")        // tinytext_col
      assert(row.getString(19) == "This is a sample text") // text_col
      assert(row.getString(20) == "medium text data")      // mediumtext_col
      assert(row.getString(21) == "long text data")        // longtext_col
      val binaryCol = row.get(22).asInstanceOf[Array[Byte]]
      assert(
        binaryCol sameElements padWithNulls("binarydat".getBytes(StandardCharsets.UTF_8), 15)
      ) // binary_col
      val varbinaryCol = row.get(23).asInstanceOf[Array[Byte]]
      assert(
        varbinaryCol sameElements "varbin_dat".getBytes(StandardCharsets.UTF_8)
      ) // varbinary_col
      val tinyblobCol = row.get(24).asInstanceOf[Array[Byte]]
      assert(
        tinyblobCol sameElements "tinyblob_data".getBytes(StandardCharsets.UTF_8)
      ) // tinyblob_col
      val blobCol = row.get(25).asInstanceOf[Array[Byte]]
      assert(blobCol sameElements "blob_data".getBytes(StandardCharsets.UTF_8)) // blob_col
      val mediumblobCol = row.get(26).asInstanceOf[Array[Byte]]
      assert(
        mediumblobCol sameElements "mediumblob_data".getBytes(StandardCharsets.UTF_8)
      ) // mediumblob_col
      val longblobCol = row.get(27).asInstanceOf[Array[Byte]]
      assert(
        longblobCol sameElements "longblob_data".getBytes(StandardCharsets.UTF_8)
      )                                                                    // longblob_col
      assert(row.getDate(28).toLocalDate == LocalDate.parse("2024-10-27")) // date_col
      // Note: Spark returns the LocalDateTime converted to UTC for some reason.
      // This is a bit weird. The same issue occurs with Databricks and Snowflake
      val timeCol = row.get(29).asInstanceOf[LocalDateTime]
      assert(
        timeCol == DateTimeUtil.localToUtc(DateTimeUtil.parseLocalTime("12:34:56"))
      ) // time_col
      val datetimeCol = row.get(30).asInstanceOf[LocalDateTime]
      assert(
        datetimeCol == DateTimeUtil.localToUtc(LocalDateTime.parse("2024-10-27T12:34:56"))
      ) // datetime_col
      assert(
        row.getTimestamp(31).toLocalDateTime == LocalDateTime.parse("2024-10-27T12:34:56")
      ) // timestamp_col
    }
  }

  private def padWithNulls(bytes: Array[Byte], length: Int): Array[Byte] = {
    val padded = new Array[Byte](length)
    System.arraycopy(bytes, 0, padded, 0, bytes.length)
    padded
  }
}
