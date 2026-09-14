/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.model;

import java.util.List;
import java.util.UUID;
import lombok.Value;

@Value
public class Namespace {
  UUID id;
  UUID dataSourceId;
  List<String> names;

  public static Namespace create(UUID dataSourceId, List<String> names) {
    return new Namespace(UUID.randomUUID(), dataSourceId, names);
  }
}
