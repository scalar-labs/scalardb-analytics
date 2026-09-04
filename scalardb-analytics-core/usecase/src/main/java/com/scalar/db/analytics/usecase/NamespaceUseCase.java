/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase;

import com.scalar.db.analytics.api.model.DataSourceNamespace;
import com.scalar.db.analytics.api.model.Namespace;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NamespaceUseCase {
  List<DataSourceNamespace> listNamespaces(UUID userId, String catalogName);

  Optional<Namespace> describeNamespace(
      UUID userId, String catalogName, String dataSourceName, List<String> namespaceNames);

  Optional<Namespace> describeNamespaceById(UUID userId, UUID namespaceId);
}
