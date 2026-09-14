/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.datasource.scalardb.schema;

import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.api.model.DataTypeKind;
import com.scalar.db.analytics.api.model.TableDetail;
import com.scalar.db.analytics.lib.exception.NonExhaustiveEnumSwitchException;

public class TypeMapping {
  public static com.scalar.db.io.DataType[] toScalarDbTypes(TableDetail detail) {
    return detail.getColumns().stream()
        .map(column -> TypeMapping.toScalarDbType(column.getType()))
        .toArray(com.scalar.db.io.DataType[]::new);
  }

  public static com.scalar.db.io.DataType toScalarDbType(DataType type) {
    DataTypeKind kind = type.getKind();
    switch (kind) {
      case BOOLEAN:
        return com.scalar.db.io.DataType.BOOLEAN;
      case INT:
        return com.scalar.db.io.DataType.INT;
      case BIGINT:
        return com.scalar.db.io.DataType.BIGINT;
      case FLOAT:
        return com.scalar.db.io.DataType.FLOAT;
      case DOUBLE:
        return com.scalar.db.io.DataType.DOUBLE;
      case TEXT:
        return com.scalar.db.io.DataType.TEXT;
      case BLOB:
        return com.scalar.db.io.DataType.BLOB;
      case DATE:
        return com.scalar.db.io.DataType.DATE;
      case TIME:
        return com.scalar.db.io.DataType.TIME;
      case TIMESTAMP:
        return com.scalar.db.io.DataType.TIMESTAMP;
      case TIMESTAMPTZ:
        return com.scalar.db.io.DataType.TIMESTAMPTZ;
      default:
        throw new IllegalArgumentException("Unsupported data type for ScalarDB: " + type);
    }
  }

  public static DataType toModelType(com.scalar.db.io.DataType type) {
    switch (type) {
      case BOOLEAN:
        return DataType.Boolean.INSTANCE;
      case INT:
        return DataType.Int.INSTANCE;
      case BIGINT:
        return DataType.BigInt.INSTANCE;
      case FLOAT:
        return DataType.Float.INSTANCE;
      case DOUBLE:
        return DataType.Double.INSTANCE;
      case TEXT:
        return DataType.Text.INSTANCE;
      case BLOB:
        return DataType.Blob.INSTANCE;
      case DATE:
        return DataType.Date.INSTANCE;
      case TIME:
        return DataType.Time.INSTANCE;
      case TIMESTAMP:
        return DataType.Timestamp.INSTANCE;
      case TIMESTAMPTZ:
        return DataType.TimestampTZ.INSTANCE;
    }
    throw new NonExhaustiveEnumSwitchException(com.scalar.db.io.DataType.class, type);
  }
}
