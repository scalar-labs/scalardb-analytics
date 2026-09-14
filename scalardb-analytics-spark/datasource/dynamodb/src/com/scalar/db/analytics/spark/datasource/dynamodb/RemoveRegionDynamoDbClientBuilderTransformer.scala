/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.dynamodb

import org.apache.hadoop.dynamodb.DynamoDbClientBuilderTransformer
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder

/** A transformer that removes the region from the DynamoDbClientBuilder. This is to work around an
  * issue where the Hadoop DynamoDB library always sets the region, which causes the endpoint
  * override not working.
  */
class RemoveRegionDynamoDbClientBuilderTransformer extends DynamoDbClientBuilderTransformer {
  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  override def apply(builder: DynamoDbClientBuilder): DynamoDbClientBuilder =
    builder.region(null)
}
