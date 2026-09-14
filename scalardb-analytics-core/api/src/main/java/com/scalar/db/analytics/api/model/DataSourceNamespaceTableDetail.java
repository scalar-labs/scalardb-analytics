/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.model;

import lombok.Value;

/**
 * DataSourceNamespaceTableDetail is a value object that holds TableDetail along with Namespace and
 * DataSource that the table belongs to.
 */
@Value
public class DataSourceNamespaceTableDetail {
  DataSource dataSource;
  Namespace namespace;
  TableDetail table;
}
