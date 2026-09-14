/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.dynamodb

import com.scalar.db.analytics.api.model.TableDetail
import com.scalar.db.analytics.api.model.datasource.provider.DynamoDbProvider
import com.scalar.db.analytics.spark.lib.table.SchemaConverter
import com.scalar.db.analytics.spark.lib.table.hadoop.{
  InputFormatJobConfConfigurator,
  InputFormatTableWrapper
}
import org.apache.hadoop.dynamodb.DynamoDBItemWritable
import org.apache.hadoop.dynamodb.read.DynamoDBInputFormat
import org.apache.hadoop.io.Text
import org.apache.spark.sql.connector.catalog.Table
import org.apache.spark.sql.types.StructType

/** A factory class to create a [[Table]] instance for DynamoDB. */
object DynamoDbTableFactory {
  def create(
      table: TableDetail,
      provider: DynamoDbProvider
  ): Table = {
    val schema: StructType = SchemaConverter.toSparkSchema(table)
    val converter          = new DynamoDbRecordConverter(table)

    val configurator: InputFormatJobConfConfigurator =
      DynamoDbJobConfConfigurator.create(table, provider)

    new InputFormatTableWrapper[Text, DynamoDBItemWritable](
      classOf[DynamoDBInputFormat],
      schema,
      converter.convert,
      configurator,
      table.getInfo.getName
    )
  }
}
