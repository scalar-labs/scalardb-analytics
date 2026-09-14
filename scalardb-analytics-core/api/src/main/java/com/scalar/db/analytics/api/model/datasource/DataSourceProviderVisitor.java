/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.model.datasource;

import com.scalar.db.analytics.api.model.datasource.provider.DynamoDbProvider;
import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Databricks;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.MySql;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Oracle;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Snowflake;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.SqlServer;

public interface DataSourceProviderVisitor<T> {
  T visit(MySql mySql);

  T visit(PostgreSql postgreSql);

  T visit(Oracle oracle);

  T visit(SqlServer sqlServer);

  T visit(ScalarDbProvider scalarDb);

  T visit(DynamoDbProvider dynamoDb);

  T visit(Databricks databricks);

  T visit(Snowflake snowflake);
}
