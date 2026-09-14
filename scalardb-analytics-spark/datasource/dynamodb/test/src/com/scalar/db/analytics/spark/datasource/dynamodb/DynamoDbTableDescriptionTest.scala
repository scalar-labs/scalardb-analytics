/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.dynamodb

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import software.amazon.awssdk.services.dynamodb.model.{
  BillingMode,
  BillingModeSummary,
  ProvisionedThroughputDescription,
  TableDescription
}

class DynamoDbTableDescriptionTest extends AnyFlatSpec with Matchers {

  "DynamoDbTableDescription" should "create from TableDescription with provisioned billing" in {
    val tableDescription = TableDescription
      .builder()
      .billingModeSummary(BillingModeSummary.builder().billingMode(BillingMode.PROVISIONED).build())
      .provisionedThroughput(
        ProvisionedThroughputDescription.builder().readCapacityUnits(100L).build()
      )
      .itemCount(1000L)
      .tableSizeBytes(50000L)
      .build()

    val description = DynamoDbTableDescription.fromTableDescription(tableDescription)

    description.billingMode should be(Some(BillingMode.PROVISIONED))
    description.readCapacityUnits should be(100L)
    description.itemCount should be(1000L)
    description.tableSizeBytes should be(50000L)
  }

  it should "create from TableDescription with on-demand billing" in {
    val tableDescription = TableDescription
      .builder()
      .billingModeSummary(
        BillingModeSummary.builder().billingMode(BillingMode.PAY_PER_REQUEST).build()
      )
      .provisionedThroughput(
        ProvisionedThroughputDescription.builder().readCapacityUnits(0L).build()
      )
      .itemCount(500L)
      .tableSizeBytes(25000L)
      .build()

    val description = DynamoDbTableDescription.fromTableDescription(tableDescription)

    description.billingMode should be(Some(BillingMode.PAY_PER_REQUEST))
    description.readCapacityUnits should be(0L)
    description.itemCount should be(500L)
    description.tableSizeBytes should be(25000L)
  }

  it should "check billing mode correctly" in {
    val provisionedDescription = DynamoDbTableDescription(
      billingMode = Some(BillingMode.PROVISIONED),
      readCapacityUnits = 100L,
      itemCount = 1000L,
      tableSizeBytes = 50000L,
      averageItemSizeBytes = 50L
    )

    val onDemandDescription = DynamoDbTableDescription(
      billingMode = Some(BillingMode.PAY_PER_REQUEST),
      readCapacityUnits = 0L,
      itemCount = 500L,
      tableSizeBytes = 25000L,
      averageItemSizeBytes = 50L
    )

    val noBillingModeDescription = DynamoDbTableDescription(
      billingMode = None,
      readCapacityUnits = 100L,
      itemCount = 1000L,
      tableSizeBytes = 50000L,
      averageItemSizeBytes = 50L
    )

    provisionedDescription.isBillingMode(BillingMode.PROVISIONED) should be(true)
    provisionedDescription.isBillingMode(BillingMode.PAY_PER_REQUEST) should be(false)

    onDemandDescription.isBillingMode(BillingMode.PAY_PER_REQUEST) should be(true)
    onDemandDescription.isBillingMode(BillingMode.PROVISIONED) should be(false)

    noBillingModeDescription.isBillingMode(BillingMode.PROVISIONED) should be(false)
    noBillingModeDescription.isBillingMode(BillingMode.PAY_PER_REQUEST) should be(false)
  }
}
