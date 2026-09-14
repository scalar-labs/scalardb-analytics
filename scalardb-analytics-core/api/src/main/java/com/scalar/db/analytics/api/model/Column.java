/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.model;

import java.io.Serializable;
import java.util.UUID;
import lombok.Value;

@Value
public class Column implements Serializable {
  private static final long serialVersionUID = 1L;

  UUID id;
  UUID tableId;
  String name;
  DataType type;
  int ordinalPosition;
  boolean nullable;

  public static Column create(
      UUID tableId, String name, DataType type, int index, boolean nullable) {
    return new Column(UUID.randomUUID(), tableId, name, type, index, nullable);
  }
}
