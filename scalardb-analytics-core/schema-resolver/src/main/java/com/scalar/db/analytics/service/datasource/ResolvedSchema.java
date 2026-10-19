/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource;

import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.SkippedColumn;
import com.scalar.db.analytics.api.model.TableDetail;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Value;

/**
 * ResolvedSchema is a container class that holds the result of the schema resolution process. It
 * contains a list of ResolvedSchemaEntry objects, each of which contains a Namespace and a list of
 * TableDetail that belong to the namespace, and the source columns that were left out of the
 * resolved tables.
 */
@Value
public class ResolvedSchema {
  List<ResolvedSchemaEntry> entries;
  List<SkippedColumn> skippedColumns;

  public ResolvedSchema(List<ResolvedSchemaEntry> entries, List<SkippedColumn> skippedColumns) {
    this.entries = entries;
    this.skippedColumns = skippedColumns;
  }

  /**
   * Creates a resolved schema in which no source column was skipped.
   *
   * @param entries the resolved namespaces and their tables
   */
  public ResolvedSchema(List<ResolvedSchemaEntry> entries) {
    this(entries, List.of());
  }

  /**
   * Returns the list of Namespace objects that are resolved.
   *
   * @return the list of Namespace objects
   */
  public List<Namespace> getNamespaces() {
    return entries.stream().map(ResolvedSchemaEntry::getNamespace).collect(Collectors.toList());
  }

  /**
   * Returns the list of TableDetail objects that are resolved.
   *
   * @return the list of TableDetail objects
   */
  public List<TableDetail> getTables() {
    return entries.stream()
        .map(ResolvedSchemaEntry::getTables)
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }
}
