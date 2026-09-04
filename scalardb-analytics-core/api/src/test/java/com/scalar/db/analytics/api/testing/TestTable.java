/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.testing;

import java.util.List;

/**
 * A utility class to represent a table with its fully qualified name. The fully qualified name is
 * represented as a list of strings, where the last element is the table name and the preceding
 * elements are the namespace components.
 */
public class TestTable {

  private final List<String> fullyQualifiedName;

  private TestTable(String... fullyQualifiedName) {
    this.fullyQualifiedName = java.util.Arrays.asList(fullyQualifiedName);
  }

  /**
   * Creates a new instance of TestTable with the given fully qualified name.
   *
   * @param fullyQualifiedName the last element is the table name and the preceding elements are the
   *     namespace components.
   * @return a new instance of TestTable
   */
  public static TestTable of(String... fullyQualifiedName) {
    return new TestTable(fullyQualifiedName);
  }

  /**
   * Returns the namespace as a list of strings which is the fully qualified name excluding the last
   * element
   *
   * @return the namespace as a list of strings
   */
  public List<String> getNamespace() {
    return fullyQualifiedName.subList(0, fullyQualifiedName.size() - 1);
  }

  /**
   * Returns the table name, which is the last element of the fully qualified name.
   *
   * @return the table name
   */
  public String getTableName() {
    return fullyQualifiedName.get(fullyQualifiedName.size() - 1);
  }

  /**
   * Returns the fully qualified name of the table as a string, which is the concatenation of all
   * elements in the fully qualified name separated by dots.
   *
   * @return the fully qualified name
   */
  public String getFullyQualifiedName() {
    return String.join(".", fullyQualifiedName);
  }

  @Override
  public String toString() {
    return "Table{" + "fullyQualifiedName=" + getFullyQualifiedName() + '}';
  }
}
