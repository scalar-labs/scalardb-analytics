/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Marker interface for data type representations in ScalarDB Analytics.
 *
 * <p>This interface serves as the base type for all data type implementations, each representing a
 * specific database data type with its associated metadata and constraints. Implementations include
 * numeric types (e.g., {@code Int}, {@code Decimal}), text types (e.g., {@code Text}, {@code
 * Blob}), temporal types (e.g., {@code Date}, {@code Timestamp}), and other specialized types.
 */
public interface DataType {
  /**
   * Returns the canonical data type identifier (e.g. {@code "INT"}, {@code "TEXT"}).
   *
   * @return Data type identifier.
   */
  DataTypeKind getKind();

  @Builder
  @Value
  @Jacksonized
  static class BigInt implements DataType {
    public static BigInt INSTANCE = new BigInt();
    DataTypeKind kind = DataTypeKind.BIGINT;
  }

  @Builder
  @Value
  @Jacksonized
  static class Blob implements DataType {
    public static Blob INSTANCE = new Blob();
    DataTypeKind kind = DataTypeKind.BLOB;
  }

  @Builder
  @Value
  @Jacksonized
  static class Boolean implements DataType {
    public static Boolean INSTANCE = new Boolean();
    DataTypeKind kind = DataTypeKind.BOOLEAN;
  }

  @Builder
  @Value
  @Jacksonized
  static class Byte implements DataType {
    public static Byte INSTANCE = new Byte();
    DataTypeKind kind = DataTypeKind.BYTE;
  }

  @Builder
  @Value
  @Jacksonized
  static class Date implements DataType {
    public static Date INSTANCE = new Date();
    DataTypeKind kind = DataTypeKind.DATE;
  }

  @Value
  @Builder
  @Jacksonized
  static class Decimal implements DataType {
    /**
     * The default precision is 38 and scale is 0, which matches common database defaults (e.g.,
     * PostgreSQL DECIMAL without precision) and is used when deserializing string literal format
     * like "DECIMAL" without explicit precision and scale values.
     */
    public static Decimal DEFAULT_INSTANCE = new Decimal(38, 0);

    DataTypeKind kind = DataTypeKind.DECIMAL;
    int precision;
    int scale;
  }

  @Builder
  @Value
  @Jacksonized
  static class Double implements DataType {
    public static Double INSTANCE = new Double();
    DataTypeKind kind = DataTypeKind.DOUBLE;
  }

  @Builder
  @Value
  @Jacksonized
  static class Duration implements DataType {
    public static Duration INSTANCE = new Duration();
    DataTypeKind kind = DataTypeKind.DURATION;
  }

  @Builder
  @Value
  @Jacksonized
  static class Float implements DataType {
    public static Float INSTANCE = new Float();
    DataTypeKind kind = DataTypeKind.FLOAT;
  }

  @Builder
  @Value
  @Jacksonized
  static class Int implements DataType {
    public static Int INSTANCE = new Int();
    DataTypeKind kind = DataTypeKind.INT;
  }

  @Builder
  @Value
  @Jacksonized
  static class Interval implements DataType {
    public static Interval INSTANCE = new Interval();
    DataTypeKind kind = DataTypeKind.INTERVAL;
  }

  @Builder
  @Value
  @Jacksonized
  static class SmallInt implements DataType {
    public static SmallInt INSTANCE = new SmallInt();
    DataTypeKind kind = DataTypeKind.SMALLINT;
  }

  @Builder
  @Value
  @Jacksonized
  static class Text implements DataType {
    public static Text INSTANCE = new Text();
    DataTypeKind kind = DataTypeKind.TEXT;
  }

  @Builder
  @Value
  @Jacksonized
  static class Time implements DataType {
    public static Time INSTANCE = new Time();
    DataTypeKind kind = DataTypeKind.TIME;
  }

  @Builder
  @Value
  @Jacksonized
  static class Timestamp implements DataType {
    public static Timestamp INSTANCE = new Timestamp();
    DataTypeKind kind = DataTypeKind.TIMESTAMP;
  }

  @Builder
  @Value
  @Jacksonized
  static class TimestampTZ implements DataType {
    public static TimestampTZ INSTANCE = new TimestampTZ();
    DataTypeKind kind = DataTypeKind.TIMESTAMPTZ;
  }
}
