/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class RdbmsSchemaResolverOption {
  public static final RdbmsSchemaResolverOption DEFAULT = getDefault();

  /**
   * Only the namespaces in this set are resolved. If this set is empty, all the namespaces are
   * resolved. Default is an empty set.
   */
  @Singular("namespaceToResolve")
  Set<List<String>> namespacesToResolve;

  /**
   * Only the tables in this set are resolved. If this set is empty, all the tables are resolved.
   * Default is an empty set.
   */
  @Singular("tableToResolve")
  Set<String> tablesToResolve;

  /**
   * If true, the resolver fails when it encounters an unsupported data type. If false, the resolver
   * ignores such data types and puts warnings in the log. Default is false.
   */
  @Builder.Default Boolean failOnUnsupportedDataType = false;

  private static RdbmsSchemaResolverOption getDefault() {
    return RdbmsSchemaResolverOption.builder().build();
  }
}
