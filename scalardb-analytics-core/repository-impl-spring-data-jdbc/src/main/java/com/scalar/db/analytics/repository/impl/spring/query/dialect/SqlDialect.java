/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.query.dialect;

import org.springframework.stereotype.Component;

/**
 * Simplified SQL dialect abstraction for ScalarDB SQL. Since ScalarDB SQL normalizes query syntax
 * across backing databases, we treat JSON/array columns as JSON text stored in regular text
 * columns.
 */
@Component
public class SqlDialect {

  public String jsonEquals(String columnName, String parameterPlaceholder) {
    return columnName + " = " + parameterPlaceholder;
  }

  public String jsonArrayContains(String columnName, String parameterPlaceholder) {
    return columnName + " = " + parameterPlaceholder;
  }

  public String arrayEquals(String columnName, String parameterPlaceholder) {
    return columnName + " = " + parameterPlaceholder;
  }

  public String quoteIdentifier(String columnName) {
    return "\"" + columnName + "\"";
  }
}
