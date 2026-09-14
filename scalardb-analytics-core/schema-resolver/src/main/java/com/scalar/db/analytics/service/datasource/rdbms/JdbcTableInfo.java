/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import java.util.List;
import lombok.Value;

/**
 * This class provides the table information of a JDBC data source. This includes the table name and
 * the columns of the table. The reason for this class is to provide a way to hold only the table
 * information of a JDBC data source without the application-specific information, such as the id.
 */
@Value
class JdbcTableInfo {
  String name;
  List<JdbcColumnInfo> columns;
}
