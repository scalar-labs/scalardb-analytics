/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource;

import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.TableDetail;
import java.util.List;
import lombok.Value;

/**
 * ResolvedSchemaEntry is a container class that holds the result of the schema resolution process.
 * It contains a Namespace and a list of TableDetail that belong to the namespace.
 */
@Value
public class ResolvedSchemaEntry {
  Namespace namespace;
  List<TableDetail> tables;
}
