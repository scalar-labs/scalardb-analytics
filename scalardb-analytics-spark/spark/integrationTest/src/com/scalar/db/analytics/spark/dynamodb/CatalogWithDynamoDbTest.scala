/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.dynamodb

import com.scalar.db.analytics.api.codec.CodecObjectMapperFactory
import com.scalar.db.analytics.api.codec.schema.DataSourceSchemaCodec
import com.scalar.db.analytics.api.model.datasource.provider.DynamoDbProvider
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest
import com.scalar.db.analytics.spark.util.TestContainers.GenericContainer
import com.scalar.db.analytics.spark.util.{CatalogUtil, TestImages}
import com.scalar.db.analytics.spark.{BaseSpec, CatalogTestBase}
import org.apache.spark.sql.connector.catalog.{Column, Identifier}
import org.apache.spark.sql.types.{DataTypes, DecimalType}
import org.scalatest.BeforeAndAfterAll
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model._

import java.net.URI

class CatalogWithDynamoDbTest extends BaseSpec with CatalogTestBase with BeforeAndAfterAll {

  override protected val CATALOG_NAME = "test_catalog"

  private val DATA_SOURCE_NAME = "dynamodb"
  private val TABLE_NAME       = "sample_table"

  // Schema JSON for DynamoDB data source
  private val SCHEMA_JSON =
    """{
      |  "namespaces": [
      |    {
      |      "names": ["sample_ns"],
      |      "tables": [
      |        {
      |          "name": "sample_table",
      |          "columns": [
      |            { "name": "byte_col", "type": "BYTE" },
      |            { "name": "small_int_col", "type": "SMALLINT" },
      |            { "name": "int_col", "type": "INT" },
      |            { "name": "bigint_col", "type": "BIGINT" },
      |            { "name": "boolean_col", "type": "BOOLEAN" },
      |            { "name": "float_col", "type": "FLOAT" },
      |            { "name": "double_col", "type": "DOUBLE" },
      |            { "name": "decimal_col", "type": "DECIMAL" },
      |            { "name": "text_col", "type": "TEXT" },
      |            { "name": "blob_col", "type": "BLOB" }
      |          ]
      |        }
      |      ]
      |    }
      |  ]
      |}""".stripMargin

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var dynamoDbContainer: Option[GenericContainer] = None

  override protected def beforeAll(): Unit = {
    // Set AWS credentials for DynamoDB local
    System.setProperty("aws.region", "fake-region")
    System.setProperty("aws.accessKeyId", "dummy")
    System.setProperty("aws.secretAccessKey", "dummy")

    // Start DynamoDB local container
    val dynamoDb = new GenericContainer(TestImages.DYNAMODB_LOCAL).withExposedPorts(8000)
    dynamoDb.start()
    dynamoDbContainer = Some(dynamoDb)

    // Setup DynamoDB table and test data
    setupTestTable(dynamoDb)

    // Start PostgreSQL and server containers
    startServerBundle()

    // Create catalog
    analyticsClient.catalog().createCatalog(CATALOG_NAME)

    // Register DynamoDB data source
    dynamoDbContainer.fold(
      throw new IllegalStateException("DynamoDB container not initialized")
    ) { dynamoDb =>
      val endpoint = s"http://${dynamoDb.getHost}:${dynamoDb.mappedPort(8000).toString}"
      val provider = DynamoDbProvider.builder().endpoint(endpoint).build()

      // Parse schema JSON to DataSourceSchema object
      val objectMapper = CodecObjectMapperFactory.create()
      val schemaCodec  = new DataSourceSchemaCodec(objectMapper)
      val schema       = schemaCodec.deserialize(SCHEMA_JSON)

      val request = new RegisterDataSourceRequest(
        CATALOG_NAME,
        DATA_SOURCE_NAME,
        provider,
        schema
      )

      analyticsClient.dataSource().register(request)
    }

    // Initialize Spark session after containers and data sources are ready
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    // Stop Spark session first
    super.afterAll()

    // Stop PostgreSQL and server containers
    stopServerBundle()

    // Stop DynamoDB container
    dynamoDbContainer.foreach(_.stop())
    dynamoDbContainer = None
  }

  private def setupTestTable(container: GenericContainer): Unit = {
    val endpoint = s"http://${container.getHost}:${container.mappedPort(8000).toString}"
    val ddb      = DynamoDbClient.builder().endpointOverride(URI.create(endpoint)).build()

    try {
      createTable(ddb)
      loadTestData(ddb)
    } finally
      ddb.close()
  }

  private def createTable(ddb: DynamoDbClient): Unit = {
    val createTableRequest = CreateTableRequest
      .builder()
      .attributeDefinitions(
        AttributeDefinition
          .builder()
          .attributeName("int_col")
          .attributeType(ScalarAttributeType.N)
          .build()
      )
      .keySchema(
        KeySchemaElement.builder().attributeName("int_col").keyType(KeyType.HASH).build()
      )
      .provisionedThroughput(
        ProvisionedThroughput.builder().readCapacityUnits(1L).writeCapacityUnits(1L).build()
      )
      .tableName(TABLE_NAME)
      .build()

    val dbWaiter = ddb.waiter()
    try {
      ddb.createTable(createTableRequest)
      val tableRequest = DescribeTableRequest.builder().tableName(TABLE_NAME).build()
      val _            = dbWaiter.waitUntilTableExists(tableRequest)
    } catch {
      case e: DynamoDbException =>
        throw new RuntimeException("Failed to create a table", e)
    }
  }

  private def loadTestData(ddb: DynamoDbClient): Unit = {
    val itemMap = java.util.Map.of(
      "byte_col",
      AttributeValue.builder().n(Byte.MaxValue.toString).build(),
      "small_int_col",
      AttributeValue.builder().n(Short.MaxValue.toString).build(),
      "int_col",
      AttributeValue.builder().n(Int.MaxValue.toString).build(),
      "bigint_col",
      AttributeValue.builder().n(Long.MaxValue.toString).build(),
      "float_col",
      AttributeValue.builder().n(Float.MaxValue.toString).build(),
      "double_col",
      AttributeValue.builder().n((Float.MaxValue.toDouble + 1).toString).build(),
      "decimal_col",
      AttributeValue.builder().n((BigDecimal(Long.MaxValue) + BigDecimal(1)).toString).build(),
      "text_col",
      AttributeValue.builder().s("text").build(),
      "blob_col",
      AttributeValue
        .builder()
        .b(SdkBytes.fromByteArray(Array[Byte](0x01, 0x02, 0x03, 0x04)))
        .build(),
      "boolean_col",
      AttributeValue.builder().bool(true).build()
    )

    val putItemRequest = PutItemRequest.builder().tableName(TABLE_NAME).item(itemMap).build()
    val _ =
      try
        ddb.putItem(putItemRequest)
      catch {
        case e: ResourceNotFoundException =>
          throw new RuntimeException("Table not found", e)
        case e: DynamoDbException =>
          throw new RuntimeException("Failed to insert an item", e)
      }
  }

  describe("Catalog") {
    // ListNamespace tests
    it("should list namespaces") {
      val catalog    = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val namespaces = catalog.listNamespaces().map(_.mkString(".")).toSeq
      assert(namespaces == Seq("dynamodb.sample_ns"))
    }

    // ListTables tests
    it("should list tables") {
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val tables  = catalog.listTables(Array("dynamodb", "sample_ns")).map(_.toString).toSeq
      assert(tables == Seq("dynamodb.sample_ns.sample_table"))
    }

    // LoadTable tests
    it("should load table") {
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val table   = catalog.loadTable(Identifier.of(Array("dynamodb", "sample_ns"), "sample_table"))
      assert(table.name() == "sample_table")
    }

    it("should have correct columns") {
      val catalog = CatalogUtil.getCatalog(spark, CATALOG_NAME)
      val table   = catalog.loadTable(Identifier.of(Array("dynamodb", "sample_ns"), "sample_table"))
      val columns = table.columns()
      val expected = Array(
        Column.create("byte_col", DataTypes.ByteType, true),
        Column.create("small_int_col", DataTypes.ShortType, true),
        Column.create("int_col", DataTypes.IntegerType, true),
        Column.create("bigint_col", DataTypes.LongType, true),
        Column.create("boolean_col", DataTypes.BooleanType, true),
        Column.create("float_col", DataTypes.FloatType, true),
        Column.create("double_col", DataTypes.DoubleType, true),
        Column.create("decimal_col", DecimalType(38, 0), true),
        Column.create("text_col", DataTypes.StringType, true),
        Column.create("blob_col", DataTypes.BinaryType, true)
      )
      assert(columns.toSet == expected.toSet)
    }

    it("should read data") {
      val rows = spark.sql(s"SELECT * FROM $CATALOG_NAME.dynamodb.sample_ns.sample_table").collect()
      assert(rows.length == 1)

      val row = rows(0)
      assert(row.getByte(row.fieldIndex("byte_col")) == Byte.MaxValue)
      assert(row.getShort(row.fieldIndex("small_int_col")) == Short.MaxValue)
      assert(row.getInt(row.fieldIndex("int_col")) == Int.MaxValue)
      assert(row.getLong(row.fieldIndex("bigint_col")) == Long.MaxValue)
      assert(row.getBoolean(row.fieldIndex("boolean_col")) == true)
      assert(row.getFloat(row.fieldIndex("float_col")) == Float.MaxValue)
      assert(row.getDouble(row.fieldIndex("double_col")) == Float.MaxValue.toDouble + 1)
      assert(
        BigDecimal(row.getDecimal(row.fieldIndex("decimal_col"))) == BigDecimal(
          Long.MaxValue
        ) + BigDecimal(1)
      )
      assert(row.getString(row.fieldIndex("text_col")) == "text")
      assert(
        row.get(row.fieldIndex("blob_col")).asInstanceOf[Array[Byte]] sameElements Array[Byte](
          0x01,
          0x02,
          0x03,
          0x04
        )
      )
    }
  }
}
