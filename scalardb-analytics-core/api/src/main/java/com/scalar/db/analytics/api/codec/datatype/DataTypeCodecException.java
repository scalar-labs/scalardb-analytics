/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.codec.datatype;

/**
 * Exception thrown when data type codec operations fail.
 *
 * <p>This exception indicates a failure during serialization or deserialization of {@link
 * com.scalar.db.analytics.model.datatype.DataType} instances. Common causes include invalid JSON
 * structure, missing required fields, or internal codec errors.
 */
public class DataTypeCodecException extends RuntimeException {

  /**
   * Constructs a new DataTypeCodecException with the specified detail message.
   *
   * @param message the detail message
   */
  public DataTypeCodecException(String message) {
    super(message);
  }

  /**
   * Constructs a new DataTypeCodecException with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public DataTypeCodecException(String message, Throwable cause) {
    super(message, cause);
  }
}
