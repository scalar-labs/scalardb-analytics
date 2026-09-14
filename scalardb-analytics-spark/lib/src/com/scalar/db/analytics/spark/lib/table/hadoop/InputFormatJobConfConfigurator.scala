/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.lib.table.hadoop

import org.apache.hadoop.mapred.JobConf

/** A configurator for JobConf to set up the input format. This is required because the
  * PartitionReaderFactory requires all its members to be serializable, and we need JobConf to
  * instantiate InputFormat and RecordReader for PartitionReader, while JobConf itself is not
  * serializable. So, we hold this configurator as a serializable object and apply it to the JobConf
  * when needed.
  */
trait InputFormatJobConfConfigurator extends Serializable {
  def accept(jobConf: JobConf): Unit
}
