/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.scalardb.table

@SuppressWarnings(Array("org.wartremover.warts.Null", "org.wartremover.warts.DefaultArguments"))
class ScalarDbException(message: String, cause: Throwable = null)
    extends RuntimeException(message, cause)
    with Serializable {

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  def this(message: String) = this(message, null)
}
