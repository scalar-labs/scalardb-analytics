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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A visitor interface for the DataSourceProvider hierarchy. This provides a default implementation
 * for each visit method that does nothing. This is useful when you want to implement only a subset
 * of the visit methods.
 */
@SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION")
public interface NoopDataSourceProviderVisitor extends DataSourceProviderVisitor<Void> {
  @Override
  default Void visit(MySql mySql) {
    return null;
  }

  @Override
  default Void visit(PostgreSql postgreSql) {
    return null;
  }

  @Override
  default Void visit(Oracle oracle) {
    return null;
  }

  @Override
  default Void visit(SqlServer sqlServer) {
    return null;
  }

  @Override
  default Void visit(ScalarDbProvider scalarDb) {
    return null;
  }

  @Override
  default Void visit(DynamoDbProvider dynamoDb) {
    return null;
  }

  @Override
  default Void visit(Databricks databricks) {
    return null;
  }

  @Override
  default Void visit(Snowflake snowflake) {
    return null;
  }
}
