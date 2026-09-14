/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.compat.spark

// This class is added in Spark 3.5
import org.apache.spark.sql.catalyst.util.{QuotingUtils => SparkQuotingUtils}

object QuotingUtil {
  def quoted(namespace: Array[String]): String =
    SparkQuotingUtils.quoted(namespace)

  def quoteIfNeeded(part: String): String =
    SparkQuotingUtils.quoteIfNeeded(part)
}
