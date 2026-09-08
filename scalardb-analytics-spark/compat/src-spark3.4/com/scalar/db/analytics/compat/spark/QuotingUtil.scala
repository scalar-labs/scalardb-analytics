/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.compat.spark

// The implementations are copied from org.apache.spark.sql.catalyst.util.QuotingUtils of Spark 3.5
// to make it compatible with Spark 3.4.
object QuotingUtil {
  def quoted(namespace: Array[String]): String =
    namespace.map(quoteIfNeeded).mkString(".")

  def quoteIfNeeded(part: String): String =
    // Quote if contains special characters or is all digits
    // Don't quote valid identifiers (alphanumeric + underscore, not all digits)
    if (part.matches("[a-zA-Z0-9_]+") && !part.matches("\\d+")) {
      part
    } else {
      s"`${part.replace("`", "``")}`"
    }
}
