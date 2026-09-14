/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.dynamodb

import software.amazon.awssdk.services.dynamodb.model.{BillingMode, TableDescription}

case class DynamoDbTableDescription(
    billingMode: Option[BillingMode],
    readCapacityUnits: Long,
    itemCount: Long,
    tableSizeBytes: Long,
    averageItemSizeBytes: Long
) {
  def isBillingMode(mode: BillingMode): Boolean = billingMode.contains(mode)
}

object DynamoDbTableDescription {
  def fromTableDescription(tableDescription: TableDescription): DynamoDbTableDescription =
    DynamoDbTableDescription(
      billingMode = Option(tableDescription.billingModeSummary()).map(_.billingMode()),
      readCapacityUnits = Option(tableDescription.provisionedThroughput())
        .flatMap(pt => Option(pt.readCapacityUnits()))
        .map(_.longValue())
        .getOrElse(0L),
      itemCount = tableDescription.itemCount(),
      tableSizeBytes = tableDescription.tableSizeBytes(),
      averageItemSizeBytes =
        if (tableDescription.itemCount() == 0) 0L
        else tableDescription.tableSizeBytes() / tableDescription.itemCount()
    )
}
