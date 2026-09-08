/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.scalardb

import com.google.common.io.Resources
import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest
import com.scalar.db.analytics.spark.{BaseSpec, CatalogTestBase}
import com.scalar.db.analytics.spark.util.{CatalogUtil, DateTimeUtil}
import com.scalar.db.api.Insert
import com.scalar.db.io.Key
import com.scalar.db.schemaloader.SchemaLoader
import com.scalar.db.service.TransactionFactory
import org.apache.spark.sql.connector.catalog.{Column, Identifier}
import org.apache.spark.sql.types.DataTypes
import org.scalatest.BeforeAndAfterAll

import java.nio.file.Paths
import java.time.{LocalDate, LocalDateTime, LocalTime, ZoneOffset}
import scala.jdk.CollectionConverters._

/** Base class for ScalarDB integration tests.
  *
  * This class provides common test infrastructure for testing ScalarDB data sources:
  *   - Analytics Server and Client lifecycle management
  *   - ScalarDB data source registration
  *   - Schema loading and test data insertion
  *   - Common test cases for namespace, table, and data operations
  *
  * Concrete test classes should:
  *   1. Extend this class 2. Start database containers (MySQL, PostgreSQL, etc.) 3. Implement
  *      getScalarDbConfigs() to provide database-specific ScalarDB configuration
  */
abstract class ScalarDbTestBase extends BaseSpec with CatalogTestBase with BeforeAndAfterAll {

  override protected val CATALOG_NAME = "test_catalog"
  private val DATA_SOURCE_NAME        = "scalardb"
  private val TABLE_FQN               = s"$CATALOG_NAME.$DATA_SOURCE_NAME.sample_db.sample_table"
  private val SCHEMA_RESOURCE         = "scalardb/schema.json"

  // Test data constants
  private val TEST_DATE      = LocalDate.of(2025, 2, 19)
  private val TEST_TIME      = LocalTime.of(12, 34, 56)
  private val TEST_TIMESTAMP = LocalDateTime.of(2025, 2, 19, 12, 34, 56)
  private val TEST_TIMESTAMP_TZ =
    LocalDateTime.of(2025, 2, 19, 12, 34, 56).toInstant(ZoneOffset.UTC)

  /** Returns ScalarDB configuration as a Map. Concrete test classes must implement this method to
    * provide database-specific ScalarDB configuration.
    *
    * Example for MySQL:
    * {{{
    * Map(
    *   "scalar.db.storage" -> "jdbc",
    *   "scalar.db.contact_points" -> container.getJdbcUrl,
    *   "scalar.db.username" -> container.getUsername,
    *   "scalar.db.password" -> container.getPassword
    * )
    * }}}
    *
    * @return
    *   ScalarDB configuration map
    */
  protected def getScalarDbConfigs(): Map[String, String]

  /** Returns additional options for SchemaLoader. Override this method if you need to pass
    * additional options to SchemaLoader (e.g., for DynamoDB or Cassandra).
    *
    * @return
    *   SchemaLoader options map
    */
  protected def getSchemaLoaderOptions(): Map[String, String] = Map.empty[String, String]

  override protected def beforeAll(): Unit = {
    // Start PostgreSQL and server containers
    startServerBundle()

    // Create catalog
    analyticsClient.catalog().createCatalog(CATALOG_NAME)

    // Load schema and test data (must be done before registering data source)
    loadSchemaAndTestData()

    // Register ScalarDB data source (will recognize the schema loaded above)
    registerScalarDbDataSource()

    // Initialize Spark session after containers and data sources are ready
    super.beforeAll()
  }

  override protected def afterAll(): Unit =
    try
      // Clean up Spark session first (parent class)
      super.afterAll()
    finally
      // Then clean up server and analytics client
      stopServerBundle()

  private def registerScalarDbDataSource(): Unit = {
    // Create ScalarDbProvider with configs
    val provider = ScalarDbProvider.builder().configs(getScalarDbConfigs().asJava).build()

    // Register data source (schema is null for ScalarDB)
    val request = new RegisterDataSourceRequest(
      CATALOG_NAME,
      DATA_SOURCE_NAME,
      provider,
      null
    )

    analyticsClient.dataSource().register(request): Unit
  }

  private def loadSchemaAndTestData(): Unit = {
    // Create a temporary properties file for ScalarDB configuration
    val tempConfigFile = java.nio.file.Files.createTempFile("scalardb", ".properties")
    try {
      val properties = new java.util.Properties()
      getScalarDbConfigs().foreach { case (key, value) =>
        properties.setProperty(key, value)
      }

      val writer = java.nio.file.Files.newBufferedWriter(tempConfigFile)
      try
        properties.store(writer, "ScalarDB configuration for integration test")
      finally
        writer.close()

      // Load schema using SchemaLoader
      val schemaFileUrl  = Resources.getResource(SCHEMA_RESOURCE)
      val schemaFilePath = Paths.get(schemaFileUrl.toURI)
      SchemaLoader.load(
        tempConfigFile,
        schemaFilePath,
        getSchemaLoaderOptions().asJava,
        true,
        false
      )

      // Insert test data using ScalarDB API
      insertTestData(tempConfigFile)
    } finally
      java.nio.file.Files.deleteIfExists(tempConfigFile): Unit
  }

  private def insertTestData(configPath: java.nio.file.Path): Unit = {
    val factory = TransactionFactory.create(configPath)
    val manager = factory.getTransactionManager
    try {
      val tx = manager.start()
      try {
        // Row 1: fully populated. Its values are asserted by the type/value-verification tests, so
        // they must be kept stable. Only this row matches `int_col = 1` / `text_col = 'text'`.
        val row1 = Insert
          .newBuilder()
          .namespace("sample_db")
          .table("sample_table")
          .partitionKey(Key.ofInt("int_col", 1))
          .clusteringKey(Key.ofBigInt("bigint_col", 1L))
          .booleanValue("boolean_col", true)
          .floatValue("float_col", 1.1f)
          .doubleValue("double_col", 1.1)
          .textValue("text_col", "text")
          .blobValue("blob_col", Array[Byte](0x01, 0x02, 0x03, 0x04))
          .dateValue("date_col", TEST_DATE)
          .timeValue("time_col", TEST_TIME)
          .timestampValue("timestamp_col", TEST_TIMESTAMP)
          .timestampTZValue("timestamptz_col", TEST_TIMESTAMP_TZ)
          .build()

        // Row 2: distinct int_col and a non-null text_col, used for range filters and partial
        // push-down (e.g. text_col LIKE '%er%' matches only this row).
        val row2 = Insert
          .newBuilder()
          .namespace("sample_db")
          .table("sample_table")
          .partitionKey(Key.ofInt("int_col", 2))
          .clusteringKey(Key.ofBigInt("bigint_col", 1L))
          .booleanValue("boolean_col", false)
          .textValue("text_col", "other")
          .build()

        // Row 3: text_col left null, so IS NOT NULL filters it out.
        val row3 = Insert
          .newBuilder()
          .namespace("sample_db")
          .table("sample_table")
          .partitionKey(Key.ofInt("int_col", 3))
          .clusteringKey(Key.ofBigInt("bigint_col", 1L))
          .build()

        tx.insert(row1)
        tx.insert(row2)
        tx.insert(row3)
        tx.commit()
      } catch {
        case e: Exception =>
          tx.abort()
          throw e
      }
    } finally
      manager.close()
  }

  // Test cases

  describe("ListNamespace") {
    it("should list namespaces") {
      val catalog    = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val namespaces = catalog.listNamespaces().map(_.mkString(".")).toSeq

      assert(namespaces.toSet == Set("scalardb.sample_db"))
    }
  }

  describe("ListTables") {
    it("should list tables") {
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val tables  = catalog.listTables(Array("scalardb", "sample_db")).map(_.toString).toSet

      assert(
        tables == Set(
          "scalardb.sample_db.sample_table",
          "scalardb.sample_db.sample_table2"
        )
      )
    }
  }

  describe("LoadTable") {
    it("should load table") {
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val table =
        catalog.loadTable(Identifier.of(Array("scalardb", "sample_db"), "sample_table"))

      assert(table.name() == "scalardb.sample_db.sample_table")
    }

    describe("TransactionEnabledTable") {
      it("should have correct columns") {
        val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
        val table =
          catalog.loadTable(Identifier.of(Array("scalardb", "sample_db"), "sample_table"))

        val columns = table.columns().toSeq

        // Expected columns
        val expectedColumns = Seq(
          Column.create("int_col", DataTypes.IntegerType, false),
          Column.create("bigint_col", DataTypes.LongType, false),
          Column.create("boolean_col", DataTypes.BooleanType, true),
          Column.create("float_col", DataTypes.FloatType, true),
          Column.create("double_col", DataTypes.DoubleType, true),
          Column.create("text_col", DataTypes.StringType, true),
          Column.create("blob_col", DataTypes.BinaryType, true),
          Column.create("date_col", DataTypes.DateType, true),
          Column.create("time_col", DataTypes.TimestampNTZType, true),
          Column.create("timestamp_col", DataTypes.TimestampNTZType, true),
          Column.create("timestamptz_col", DataTypes.TimestampType, true)
        )

        val actualColumns = columns.map(c => (c.name(), c.dataType(), c.nullable())).toSet
        val expectedColumnTuples =
          expectedColumns.map(c => (c.name(), c.dataType(), c.nullable())).toSet
        assert(actualColumns == expectedColumnTuples)
      }

      it("should read data") {
        val rows = spark
          .sql("SELECT * FROM test_catalog.scalardb.sample_db.sample_table WHERE int_col = 1")
          .collect()

        assert(rows.length == 1)
        val row = rows(0)

        // Primary key and clustering key columns
        assert(row.getInt(row.fieldIndex("int_col")) == 1)
        assert(row.getLong(row.fieldIndex("bigint_col")) == 1L)

        // User-defined columns
        assert(row.getBoolean(row.fieldIndex("boolean_col")))
        assert(row.getFloat(row.fieldIndex("float_col")) == 1.1f)
        assert(row.getDouble(row.fieldIndex("double_col")) == 1.1)
        assert(row.getString(row.fieldIndex("text_col")) == "text")
        assert(
          row.get(row.fieldIndex("blob_col")).asInstanceOf[Array[Byte]] sameElements Array[Byte](
            0x01,
            0x02,
            0x03,
            0x04
          )
        )
        assert(row.getDate(row.fieldIndex("date_col")).toLocalDate == TEST_DATE)
        assert(
          row.get(row.fieldIndex("time_col")) == DateTimeUtil.toLocalDateTimeWithEpochDate(
            TEST_TIME
          )
        )
        assert(
          row.get(row.fieldIndex("timestamp_col")).asInstanceOf[LocalDateTime] == TEST_TIMESTAMP
        )
        assert(row.getTimestamp(row.fieldIndex("timestamptz_col")).toInstant == TEST_TIMESTAMP_TZ)
      }
    }
  }

  describe("CacheTable") {
    it("should use InMemoryTableScan after caching") {
      val tableFqn = s"$CATALOG_NAME.$DATA_SOURCE_NAME.sample_db.sample_table"

      spark.sql(s"CACHE TABLE $tableFqn")
      try {
        val df = spark.sql(s"SELECT * FROM $tableFqn")
        // Execute to finalize the plan (needed when AQE is enabled)
        df.collect()
        val planText = df.queryExecution.explainString(
          org.apache.spark.sql.execution.ExplainMode.fromString("extended")
        )
        assert(
          planText.contains("InMemoryTableScan") || planText.contains("InMemoryRelation"),
          "Expected InMemoryTableScan/InMemoryRelation in plan: " + planText
        )
      } finally
        spark.sql(s"UNCACHE TABLE IF EXISTS $tableFqn"): Unit
    }
  }

  describe("PushDown") {

    /** Returns the physical plan text. The DataFrame must already be executed (e.g. via collect())
      * to finalize the plan when AQE is enabled.
      */
    def executedPlanText(df: org.apache.spark.sql.DataFrame): String =
      df.queryExecution.executedPlan.toString()

    describe("Filter") {
      it("should push down an equality predicate and return matching rows") {
        val df   = spark.sql(s"SELECT * FROM $TABLE_FQN WHERE int_col = 1")
        val rows = df.collect()
        val plan = executedPlanText(df)
        assert(rows.length == 1)
        assert(rows(0).getInt(rows(0).fieldIndex("int_col")) == 1)
        assert(plan.contains("PushedPredicates:"), s"Filter not pushed down: $plan")
        assert(plan.contains("int_col = 1"), s"EQ predicate missing: $plan")
      }

      it("should push down an equality predicate and return no rows when not matched") {
        val df   = spark.sql(s"SELECT * FROM $TABLE_FQN WHERE int_col = 999")
        val rows = df.collect()
        val plan = executedPlanText(df)
        assert(rows.length == 0)
        assert(plan.contains("PushedPredicates:"), s"Filter not pushed down: $plan")
      }

      it("should push down a string equality predicate") {
        val df   = spark.sql(s"SELECT * FROM $TABLE_FQN WHERE text_col = 'text'")
        val rows = df.collect()
        val plan = executedPlanText(df)
        assert(rows.length == 1)
        assert(rows(0).getString(rows(0).fieldIndex("text_col")) == "text")
        assert(plan.contains("text_col = text"), s"EQ predicate missing: $plan")
      }

      it("should return correct results for a not-equal predicate") {
        // Note: Spark may rewrite <> as NOT(=) and handle it as a post-filter instead of
        // pushing it down. The result correctness is still verified. No row has int_col = 999, so
        // all three rows are returned.
        val df   = spark.sql(s"SELECT * FROM $TABLE_FQN WHERE int_col <> 999")
        val rows = df.collect()
        assert(rows.length == 3)
      }

      it("should push down comparison predicates") {
        // int_col >= 1 matches all three rows.
        val df   = spark.sql(s"SELECT * FROM $TABLE_FQN WHERE int_col >= 1")
        val rows = df.collect()
        val plan = executedPlanText(df)
        assert(rows.length == 3)
        assert(plan.contains("int_col >= 1"), s"GTE predicate missing: $plan")
      }

      it("should push down a comparison predicate that filters to a subset of rows") {
        // int_col >= 2 matches Row2 and Row3, but not Row1.
        val df   = spark.sql(s"SELECT * FROM $TABLE_FQN WHERE int_col >= 2")
        val rows = df.collect()
        val plan = executedPlanText(df)
        assert(rows.length == 2)
        val intCols = rows.map(r => r.getInt(r.fieldIndex("int_col"))).toSet
        assert(intCols == Set(2, 3), s"unexpected int_col values: ${intCols.mkString(", ")}")
        assert(plan.contains("int_col >= 2"), s"GTE predicate missing: $plan")
      }

      it("should push down IS NOT NULL predicate") {
        // text_col is null only in Row3, so IS NOT NULL returns Row1 and Row2.
        val df   = spark.sql(s"SELECT * FROM $TABLE_FQN WHERE text_col IS NOT NULL")
        val rows = df.collect()
        val plan = executedPlanText(df)
        assert(rows.length == 2)
        val textCols = rows.map(r => r.getString(r.fieldIndex("text_col"))).toSet
        assert(
          textCols == Set("text", "other"),
          s"unexpected text_col values: ${textCols.mkString(", ")}"
        )
        assert(
          plan.contains("text_col IS NOT NULL"),
          s"IS NOT NULL predicate missing: $plan"
        )
      }

      it("should push down multiple AND predicates") {
        val df =
          spark.sql(s"SELECT * FROM $TABLE_FQN WHERE int_col = 1 AND text_col = 'text'")
        val rows = df.collect()
        val plan = executedPlanText(df)
        assert(rows.length == 1)
        assert(
          plan.contains("int_col") && plan.contains("text_col"),
          s"AND predicates missing: $plan"
        )
      }

      it("should push down multiple AND predicates that filter out rows") {
        val df =
          spark.sql(s"SELECT * FROM $TABLE_FQN WHERE int_col = 1 AND text_col = 'nonexistent'")
        val rows = df.collect()
        assert(rows.length == 0)
      }

      it("should push down an OR predicate across columns (DNF)") {
        val df =
          spark.sql(s"SELECT * FROM $TABLE_FQN WHERE int_col = 999 OR text_col = 'text'")
        val rows = df.collect()
        val plan = executedPlanText(df)
        assert(rows.length == 1)
        assert(plan.contains("PushedPredicates:"), s"OR not pushed down: $plan")
        assert(
          plan.contains("int_col") && plan.contains("text_col"),
          s"OR predicate columns missing: $plan"
        )
      }

      it("should push down AND combined with an OR group (CNF)") {
        val df = spark.sql(
          s"SELECT * FROM $TABLE_FQN WHERE int_col = 1 AND (text_col = 'text' OR text_col = 'other')"
        )
        val rows = df.collect()
        val plan = executedPlanText(df)
        assert(rows.length == 1)
        assert(plan.contains("PushedPredicates:"), s"CNF with OR not pushed down: $plan")
        assert(
          plan.contains("int_col") && plan.contains("text_col"),
          s"CNF predicate columns missing: $plan"
        )
      }

      it("should return correct results for an OR predicate that filters out rows") {
        val df = spark.sql(
          s"SELECT * FROM $TABLE_FQN WHERE int_col = 999 OR text_col = 'nonexistent'"
        )
        val rows = df.collect()
        assert(rows.length == 0)
      }

      it("should push the supported conjunct and post-filter the unsupported one (matching)") {
        // int_col >= 1 is pushed to ScalarDB (returning all three rows), while text_col LIKE '%er%'
        // is not a supported operator and is applied by Spark as a post-filter. Only Row2 ('other')
        // matches the LIKE, so this partial push-down still yields the correct single row.
        val df = spark.sql(
          s"SELECT * FROM $TABLE_FQN WHERE int_col >= 1 AND text_col LIKE '%er%'"
        )
        val rows = df.collect()
        val plan = executedPlanText(df)
        assert(rows.length == 1)
        assert(rows(0).getString(rows(0).fieldIndex("text_col")) == "other")
        assert(
          plan.contains("PushedPredicates:") && plan.contains("int_col >= 1"),
          s"supported conjunct not pushed down: $plan"
        )
        // The LIKE is not pushed down; Spark keeps it as a post-Scan Filter.
        assert(plan.contains("Filter"), s"post-filter for LIKE missing: $plan")
      }

      it("should post-filter rows that the pushed predicate alone would return (non-matching)") {
        // int_col >= 1 pushes down and ScalarDB returns all three rows, but no text_col matches
        // LIKE '%zzz%', so Spark's post-filter removes every row. This verifies the post-filter is
        // actually applied on top of the pushed predicate (the safety guarantee of partial CNF
        // push-down).
        val df = spark.sql(
          s"SELECT * FROM $TABLE_FQN WHERE int_col >= 1 AND text_col LIKE '%zzz%'"
        )
        val rows = df.collect()
        val plan = executedPlanText(df)
        assert(rows.length == 0)
        assert(
          plan.contains("PushedPredicates:") && plan.contains("int_col"),
          s"supported conjunct not pushed down: $plan"
        )
      }

      // `PredicateConverter.createExpression` performs non-trivial value conversions for several
      // Spark types when building a `SerializablePredicate`: epoch-days <-> LocalDate,
      // epoch-micros <-> LocalDateTime/Instant/LocalTime, and UTF8String <-> String. Each of those
      // crosses the driver/executor boundary and is dispatched on the executor by ScalarDB's
      // type-specific builder methods, so a divergence between Spark's evaluation and the pushed
      // predicate would silently change results. These tests exercise each non-trivial conversion
      // against a real database to verify they remain equivalent to Spark-side evaluation. The
      // trivial conversions (Int/Long/Float/Double/Boolean/Binary) are covered by unit tests.
      describe("DataTypeConversions") {

        it("should push down a DATE equality predicate at the row's date") {
          val dateStr = TEST_DATE.toString
          val df      = spark.sql(s"SELECT * FROM $TABLE_FQN WHERE date_col = DATE '$dateStr'")
          val rows    = df.collect()
          val plan    = executedPlanText(df)
          assert(rows.length == 1)
          assert(rows(0).getInt(rows(0).fieldIndex("int_col")) == 1)
          assert(plan.contains("PushedPredicates:"), s"Filter not pushed down: $plan")
          assert(plan.contains("date_col"), s"date_col predicate missing: $plan")
        }

        it("should push down a DATE GT predicate that filters out the row") {
          val dateStr = TEST_DATE.toString
          val df      = spark.sql(s"SELECT * FROM $TABLE_FQN WHERE date_col > DATE '$dateStr'")
          val rows    = df.collect()
          val plan    = executedPlanText(df)
          assert(rows.length == 0)
          assert(plan.contains("PushedPredicates:"), s"Filter not pushed down: $plan")
        }

        it("should push down a TIMESTAMP_NTZ equality predicate against a TIMESTAMP column") {
          val ts = TEST_TIMESTAMP.toString.replace('T', ' ')
          val df =
            spark.sql(s"SELECT * FROM $TABLE_FQN WHERE timestamp_col = TIMESTAMP_NTZ '$ts'")
          val rows = df.collect()
          val plan = executedPlanText(df)
          assert(rows.length == 1)
          assert(rows(0).getInt(rows(0).fieldIndex("int_col")) == 1)
          assert(plan.contains("PushedPredicates:"), s"Filter not pushed down: $plan")
          assert(plan.contains("timestamp_col"), s"timestamp_col predicate missing: $plan")
        }

        it("should push down a TIMESTAMP_NTZ GT predicate against a TIMESTAMP column") {
          // TEST_TIMESTAMP is 2025-02-19 12:34:56; this literal is one second earlier so the row
          // matches and Spark's evaluation agrees with the pushed predicate.
          val df = spark.sql(
            s"SELECT * FROM $TABLE_FQN WHERE timestamp_col > TIMESTAMP_NTZ '2025-02-19 12:34:55'"
          )
          val rows = df.collect()
          val plan = executedPlanText(df)
          assert(rows.length == 1)
          assert(rows(0).getInt(rows(0).fieldIndex("int_col")) == 1)
          assert(plan.contains("PushedPredicates:"), s"Filter not pushed down: $plan")
        }

        it("should push down a TIMESTAMP equality predicate against a TIMESTAMPTZ column") {
          // TEST_TIMESTAMP_TZ is the UTC instant of 2025-02-19 12:34:56. Use TIMESTAMP literal
          // with an explicit UTC offset so the Spark session's local time zone does not shift the
          // literal away from the stored instant.
          val df = spark.sql(
            s"SELECT * FROM $TABLE_FQN WHERE timestamptz_col = TIMESTAMP '2025-02-19 12:34:56+00:00'"
          )
          val rows = df.collect()
          val plan = executedPlanText(df)
          assert(rows.length == 1)
          assert(rows(0).getInt(rows(0).fieldIndex("int_col")) == 1)
          assert(plan.contains("PushedPredicates:"), s"Filter not pushed down: $plan")
          assert(plan.contains("timestamptz_col"), s"timestamptz_col predicate missing: $plan")
        }

        it("should push down a TIMESTAMP GT predicate against a TIMESTAMPTZ column") {
          val df = spark.sql(
            s"SELECT * FROM $TABLE_FQN WHERE timestamptz_col > TIMESTAMP '2025-02-19 12:34:55+00:00'"
          )
          val rows = df.collect()
          val plan = executedPlanText(df)
          assert(rows.length == 1)
          assert(rows(0).getInt(rows(0).fieldIndex("int_col")) == 1)
          assert(plan.contains("PushedPredicates:"), s"Filter not pushed down: $plan")
        }

        it("should push down a string GT predicate (StringType beyond EQ)") {
          // text_col is 'text' on Row1 and 'other' on Row2; both are > 'a'.
          val df   = spark.sql(s"SELECT * FROM $TABLE_FQN WHERE text_col > 'a'")
          val rows = df.collect()
          val plan = executedPlanText(df)
          assert(rows.length == 2)
          val textCols = rows.map(r => r.getString(r.fieldIndex("text_col"))).toSet
          assert(textCols == Set("text", "other"))
          assert(plan.contains("PushedPredicates:"), s"Filter not pushed down: $plan")
          assert(plan.contains("text_col"), s"text_col predicate missing: $plan")
        }

        // TIME columns are mapped to Spark's TimestampNTZType (Spark 3.4/3.5 has no TIME type).
        // `RowConverter` wraps stored LocalTime values as `LocalDateTime(EPOCH_DATE, time)`, so
        // for Spark's TimestampNTZ comparison the row is at 1970-01-01. Push down with an
        // epoch-date literal is equivalent to Spark's evaluation; a non-epoch-date literal cannot
        // be pushed without changing the result and must be left to Spark.

        it("should push down a TIME equality predicate at the epoch date") {
          val timeStr = TEST_TIME.toString
          val df = spark.sql(
            s"SELECT * FROM $TABLE_FQN WHERE time_col = TIMESTAMP_NTZ '1970-01-01 $timeStr'"
          )
          val rows = df.collect()
          val plan = executedPlanText(df)
          assert(rows.length == 1)
          assert(rows(0).getInt(rows(0).fieldIndex("int_col")) == 1)
          assert(plan.contains("PushedPredicates:"), s"Filter not pushed down: $plan")
          assert(plan.contains("time_col"), s"time_col predicate missing: $plan")
        }

        it("should push down a TIME GT predicate at the epoch date") {
          val df = spark.sql(
            s"SELECT * FROM $TABLE_FQN WHERE time_col > TIMESTAMP_NTZ '1970-01-01 00:00:00'"
          )
          val rows = df.collect()
          val plan = executedPlanText(df)
          assert(rows.length == 1) // Only Row1 has a non-null time_col, value 12:34:56
          assert(rows(0).getInt(rows(0).fieldIndex("int_col")) == 1)
          assert(plan.contains("PushedPredicates:"), s"Filter not pushed down: $plan")
        }

        it("should reject a TIME predicate when the literal date is not the epoch date") {
          // The literal date is 2024-06-02, which is not 1970-01-01. A TIME row can only ever
          // appear at 1970-01-01 in Spark (per RowConverter), so this query has no useful answer.
          // PredicateConverter raises IllegalArgumentException at planning time so the user sees
          // the encoding constraint instead of an empty result from a wasted full scan.
          val df = spark.sql(
            s"SELECT * FROM $TABLE_FQN WHERE time_col > TIMESTAMP_NTZ '2024-06-02 00:00:00'"
          )
          val ex = intercept[IllegalArgumentException] {
            val _ = df.collect()
          }
          assert(ex.getMessage.contains("time_col"))
          assert(ex.getMessage.contains("1970-01-01"))
        }

        it("should push down TIME IS NOT NULL") {
          // Row1 has a time_col; Row2 and Row3 do not. IS_NOT_NULL goes through convertNullCheck
          // which already consults columnTypes, so this case was correct prior to Item 19; the
          // test pins the behavior alongside the new comparison-side handling.
          val df   = spark.sql(s"SELECT * FROM $TABLE_FQN WHERE time_col IS NOT NULL")
          val rows = df.collect()
          val plan = executedPlanText(df)
          assert(rows.length == 1)
          assert(rows(0).getInt(rows(0).fieldIndex("int_col")) == 1)
          assert(
            plan.contains("time_col IS NOT NULL"),
            s"IS NOT NULL predicate missing: $plan"
          )
        }
      }
    }

    describe("ColumnPruning") {
      it("should push down column selection") {
        val df   = spark.sql(s"SELECT int_col, text_col FROM $TABLE_FQN")
        val rows = df.collect()
        val plan = executedPlanText(df)
        assert(rows.length == 3)
        assert(df.schema.fieldNames.toSet == Set("int_col", "text_col"))
        assert(plan.contains("PushedColumns:"), s"Column pruning not pushed down: $plan")
        assert(
          plan.contains("int_col") && plan.contains("text_col"),
          s"Pushed columns missing: $plan"
        )
      }

      it("should return correct values with pruned columns") {
        // Filter to Row1 so the projected values are deterministic.
        val df   = spark.sql(s"SELECT int_col, double_col FROM $TABLE_FQN WHERE int_col = 1")
        val rows = df.collect()
        assert(rows.length == 1)
        assert(rows(0).getInt(0) == 1)
        assert(rows(0).getDouble(1) == 1.1)
      }

      it("should push down a single column selection") {
        val df   = spark.sql(s"SELECT text_col FROM $TABLE_FQN WHERE int_col = 1")
        val rows = df.collect()
        val plan = executedPlanText(df)
        assert(rows.length == 1)
        assert(rows(0).getString(0) == "text")
        assert(plan.contains("PushedColumns:"), s"Column pruning not pushed down: $plan")
      }
    }

    describe("Limit") {
      it("should push down limit") {
        val df   = spark.sql(s"SELECT * FROM $TABLE_FQN LIMIT 1")
        val rows = df.collect()
        val plan = executedPlanText(df)
        assert(rows.length == 1)
        assert(plan.contains("PushedLimit: 1"), s"Limit not pushed down: $plan")
      }

      it("should limit to fewer rows than the table contains") {
        // The table has three rows; LIMIT 2 must return exactly two of them.
        val df   = spark.sql(s"SELECT * FROM $TABLE_FQN LIMIT 2")
        val rows = df.collect()
        val plan = executedPlanText(df)
        assert(rows.length == 2)
        assert(plan.contains("PushedLimit: 2"), s"Limit not pushed down: $plan")
      }

      it("should return no more than the limit") {
        val df   = spark.sql(s"SELECT * FROM $TABLE_FQN LIMIT 0")
        val rows = df.collect()
        assert(rows.length == 0)
      }
    }

    describe("Combined") {
      it("should push down filter with column pruning") {
        val df   = spark.sql(s"SELECT int_col, text_col FROM $TABLE_FQN WHERE int_col = 1")
        val rows = df.collect()
        val plan = executedPlanText(df)
        assert(rows.length == 1)
        assert(rows(0).getInt(0) == 1)
        assert(rows(0).getString(1) == "text")
        assert(plan.contains("PushedPredicates:"), s"Filter not pushed down: $plan")
        assert(plan.contains("PushedColumns:"), s"Column pruning not pushed down: $plan")
      }

      it("should push down filter with column pruning and limit") {
        val df =
          spark.sql(s"SELECT int_col, text_col FROM $TABLE_FQN WHERE int_col = 1 LIMIT 1")
        val rows = df.collect()
        val plan = executedPlanText(df)
        assert(rows.length == 1)
        assert(df.schema.fieldNames.toSet == Set("int_col", "text_col"))
        assert(plan.contains("PushedPredicates:"), s"Filter not pushed down: $plan")
        assert(plan.contains("PushedColumns:"), s"Column pruning not pushed down: $plan")
        assert(plan.contains("PushedLimit:"), s"Limit not pushed down: $plan")
      }

      it("should push down non-matching filter with column pruning and limit") {
        val df =
          spark.sql(s"SELECT int_col, text_col FROM $TABLE_FQN WHERE int_col = 999 LIMIT 10")
        val rows = df.collect()
        assert(rows.length == 0)
      }
    }
  }
}
