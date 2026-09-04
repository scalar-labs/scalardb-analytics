/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.model;

import java.io.Serializable;
import java.util.UUID;
import lombok.Value;

@Value
public class TableInfo implements Serializable {
  private static final long serialVersionUID = 1L;

  UUID id;
  UUID namespaceId;
  String name;

  public static TableInfo create(UUID namespaceId, String name) {
    return new TableInfo(UUID.randomUUID(), namespaceId, name);
  }
}
