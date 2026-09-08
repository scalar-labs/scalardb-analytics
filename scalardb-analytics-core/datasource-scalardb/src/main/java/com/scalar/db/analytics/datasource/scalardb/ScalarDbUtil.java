/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.datasource.scalardb;

import com.scalar.db.analytics.api.model.TableDetail;
import com.scalar.db.transaction.consensuscommit.Attribute;

public class ScalarDbUtil {
  // ToDo: Consider replacing this with ConsensusCommitUtils.isTransactionTableMetadata
  public static boolean isTransactionEnabledTable(TableDetail table) {
    return table.getColumns().stream().anyMatch(c -> c.getName().equals(Attribute.ID));
  }
}
