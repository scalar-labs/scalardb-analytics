/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository;

import com.scalar.db.analytics.api.model.Column;
import java.util.List;
import java.util.UUID;

public interface ColumnRepository<T extends RepositoryTransactionContext> {
  List<Column> listByTableId(T ctx, UUID tableId);
}
