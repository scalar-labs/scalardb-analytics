/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.datasource.jdbc.schema;

import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class JdbcSchemaReaderOption {
  public static final JdbcSchemaReaderOption DEFAULT = getDefault();

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
   * If true, the reader fails when it encounters an unsupported data type. If false, the reader
   * ignores such data types and puts warnings in the log. Default is false.
   */
  @Builder.Default Boolean failOnUnsupportedDataType = false;

  private static JdbcSchemaReaderOption getDefault() {
    return JdbcSchemaReaderOption.builder().build();
  }
}
