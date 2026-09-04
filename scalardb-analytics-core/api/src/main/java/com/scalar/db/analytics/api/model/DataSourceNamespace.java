/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.model;

import lombok.Value;

/**
 * DataSourceNamespace is a value object that holds Namespace and DataSource that the namespace
 * belongs to.
 */
@Value
public class DataSourceNamespace {
  DataSource dataSource;
  Namespace namespace;
}
