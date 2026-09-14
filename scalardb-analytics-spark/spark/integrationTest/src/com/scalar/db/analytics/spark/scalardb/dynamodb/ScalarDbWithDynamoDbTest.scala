/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.scalardb.dynamodb

import com.scalar.db.analytics.spark.scalardb.ScalarDbTestBase
import com.scalar.db.analytics.spark.util.TestContainers.GenericContainer
import com.scalar.db.analytics.spark.util.TestImages
import com.scalar.db.storage.dynamo.DynamoAdmin
import org.scalatest.BeforeAndAfterAll

class ScalarDbWithDynamoDbTest extends ScalarDbTestBase with BeforeAndAfterAll {

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var dynamoDbContainer: Option[GenericContainer] = None

  override protected def beforeAll(): Unit = {
    // Set AWS credentials as system properties (required for DynamoDB Local)
    System.setProperty("aws.region", "fake-region")
    System.setProperty("aws.accessKeyId", "dummy")
    System.setProperty("aws.secretAccessKey", "dummy")

    // Start DynamoDB Local container
    val dynamoDb = new GenericContainer(TestImages.DYNAMODB_LOCAL).withExposedPorts(8000)
    dynamoDb.start()
    dynamoDbContainer = Some(dynamoDb)

    // Call parent beforeAll to set up Analytics Server and ScalarDB
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()

    // Stop DynamoDB container
    dynamoDbContainer.foreach(_.stop())
    dynamoDbContainer = None

    // Clean up system properties
    System.clearProperty("aws.region"): Unit
    System.clearProperty("aws.accessKeyId"): Unit
    System.clearProperty("aws.secretAccessKey"): Unit
  }

  override protected def getScalarDbConfigs(): Map[String, String] =
    dynamoDbContainer.fold(
      throw new IllegalStateException("DynamoDB container not initialized")
    ) { dynamoDb =>
      val endpoint = s"http://${dynamoDb.getHost}:${dynamoDb.mappedPort(8000).toString}"
      Map(
        "scalar.db.storage"                  -> "dynamo",
        "scalar.db.contact_points"           -> "sample",
        "scalar.db.username"                 -> "sample",
        "scalar.db.password"                 -> "sample",
        "scalar.db.dynamo.endpoint_override" -> endpoint
      )
    }

  override protected def getSchemaLoaderOptions(): Map[String, String] =
    Map(
      DynamoAdmin.NO_BACKUP  -> "true",
      DynamoAdmin.NO_SCALING -> "true"
    )
}
