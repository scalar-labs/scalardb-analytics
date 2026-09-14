/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.dynamodb

import com.scalar.db.analytics.api.model.TableDetail
import com.scalar.db.analytics.api.model.datasource.provider.DynamoDbProvider
import com.scalar.db.analytics.spark.lib.table.hadoop.InputFormatJobConfConfigurator
import org.apache.hadoop.dynamodb.{DynamoDBClient, DynamoDBConstants}
import org.apache.hadoop.mapred.JobConf
import software.amazon.awssdk.services.dynamodb.model.BillingMode

/** A configurator for JobConf to set up the DynamoDB input format. This is required because the
  * PartitionReaderFactory requires all its members to be serializable, and we need JobConf to set
  * up the DynamoDB reader for PartitionReader, while JobConf itself is not serializable. So, we
  * hold this configurator as a serializable object and apply it to the JobConf when needed.
  */
class DynamoDbJobConfConfigurator private (
    tableName: String,
    region: Option[String],
    endpoint: Option[String],
    description: DynamoDbTableDescription
) extends InputFormatJobConfConfigurator
    with Serializable {

  override def accept(jobConf: JobConf): Unit = {
    DynamoDbJobConfConfigurator.setDbProperties(jobConf, endpoint, region)
    DynamoDbJobConfConfigurator.setTableProperties(
      jobConf,
      tableName,
      description,
      DynamoDbJobConfConfigurator.DEFAULT_READ_RATIO,
      DynamoDbJobConfConfigurator.DEFAULT_SCAN_SEGMENTS
    )
  }
}

object DynamoDbJobConfConfigurator {
  // Default read ratio for the table scan. Use 1.0 to use full read capacity for now.
  private val DEFAULT_READ_RATIO = 1.0
  // Default number of segments for the table scan.
  // Use -1 to calculate automatically from the table size and the configured read capacity.
  private val DEFAULT_SCAN_SEGMENTS = -1

  // EMR Dynamodb tries to retrieve the region from the EC2 instance metadata if the region is not
  // set. This causes to mess log messages with stacktrace in the environment other than EC2, so we
  // set a fake region here.
  private val FAKE_REGION = "fake-region"

  def create(
      table: TableDetail,
      provider: DynamoDbProvider
  ): DynamoDbJobConfConfigurator = {
    val tableName = table.getInfo.getName
    val region    = Option(provider.getRegion).filter(_.nonEmpty)
    val endpoint  = Option(provider.getEndpoint).filter(_.nonEmpty)

    val jobConf = new JobConf()
    setDbProperties(jobConf, endpoint, region)

    val client = new DynamoDBClient(jobConf)
    val description =
      try
        DynamoDbTableDescription.fromTableDescription(client.describeTable(tableName))
      finally
        client.close()

    new DynamoDbJobConfConfigurator(tableName, region, endpoint, description)
  }

  private def setDbProperties(
      jobConf: JobConf,
      endpoint: Option[String],
      region: Option[String]
  ): Unit = {
    endpoint.foreach(e => jobConf.set(DynamoDBConstants.ENDPOINT, e))
    jobConf.set(DynamoDBConstants.REGION_ID, region.getOrElse(FAKE_REGION))

    /*
     * For some reason, if the region is set, overrideEndpoint() does not work as expected.
     * So, we need to remove the region from the builder explicitly to use the overridden endpoint,
     * e.g., DynamoDB Local.
     */
    jobConf.set(
      DynamoDBConstants.CUSTOM_CLIENT_BUILDER_TRANSFORMER,
      classOf[RemoveRegionDynamoDbClientBuilderTransformer].getName
    )
  }

  private def setTableProperties(
      jobConf: JobConf,
      tableName: String,
      description: DynamoDbTableDescription,
      readRatio: Double,
      totalSegments: Int
  ): Unit = {
    jobConf.set(DynamoDBConstants.INPUT_TABLE_NAME, tableName)

    if (description.billingMode.contains(BillingMode.PROVISIONED)) {
      jobConf.set(DynamoDBConstants.READ_THROUGHPUT, description.readCapacityUnits.toString)

      // Assume auto-scaling enabled for PROVISIONED tables
      jobConf.set(
        DynamoDBConstants.READ_THROUGHPUT_AUTOSCALING,
        DynamoDBConstants.DEFAULT_THROUGHPUT_AUTOSCALING
      )
    } else {
      // If not specified at the table level, set a hard-coded value of 40,000
      jobConf.set(
        DynamoDBConstants.READ_THROUGHPUT,
        DynamoDBConstants.DEFAULT_CAPACITY_FOR_ON_DEMAND.toString
      )
    }

    jobConf.set(DynamoDBConstants.ITEM_COUNT, description.itemCount.toString)
    jobConf.set(DynamoDBConstants.TABLE_SIZE_BYTES, description.tableSizeBytes.toString)
    jobConf.set(DynamoDBConstants.AVG_ITEM_SIZE, description.averageItemSizeBytes.toString)

    jobConf.set(DynamoDBConstants.THROUGHPUT_READ_PERCENT, readRatio.toString)
    jobConf.set(DynamoDBConstants.SCAN_SEGMENTS, totalSegments.toString)
  }
}
