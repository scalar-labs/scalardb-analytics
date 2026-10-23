/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource;

/**
 * Thrown when a name in a data source's schema contains a character that {@link
 * SchemaMetadataValidator} prohibits. The path and the value are escaped, so the message and the
 * accessors never contain the prohibited characters themselves.
 */
public class InvalidSchemaMetadataException extends SchemaResolverException {
  private static final long serialVersionUID = 4630905386447652916L;

  private final SchemaMetadataValidator.Field field;
  private final String path;
  private final String value;

  InvalidSchemaMetadataException(SchemaMetadataValidator.Field field, String path, String value) {
    super(
        String.format(
            "A name in the data source schema contains a prohibited control or bidirectional"
                + " formatting character. Field: %s, Path: %s, Value: %s",
            field.label(), path, value));
    this.field = field;
    this.path = path;
    this.value = value;
  }

  /** Returns the kind of name that is invalid. */
  public SchemaMetadataValidator.Field getField() {
    return field;
  }

  /**
   * Returns the escaped path of the invalid name, such as {@code "public"."orders"."a\nb"}. For a
   * column type, this is the path of the column.
   */
  public String getPath() {
    return path;
  }

  /** Returns the invalid name, escaped. */
  public String getValue() {
    return value;
  }
}
