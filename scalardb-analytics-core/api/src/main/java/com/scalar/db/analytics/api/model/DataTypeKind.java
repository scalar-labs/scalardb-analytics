/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.model;

import java.io.Serializable;

public enum DataTypeKind implements Serializable {
  BYTE,
  SMALLINT,
  INT,
  BIGINT,
  FLOAT,
  DOUBLE,
  DECIMAL,

  TEXT,
  BLOB,
  BOOLEAN,

  DATE,
  TIME,
  // Timestamp without timezone
  TIMESTAMP,
  // Timestamp with timezone
  TIMESTAMPTZ,
  DURATION,
  INTERVAL
}
