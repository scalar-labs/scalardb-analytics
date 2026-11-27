/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.datasource;

import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import com.scalar.db.analytics.api.model.datasource.DataSourceProviderVisitor;
import com.scalar.db.analytics.api.model.datasource.provider.DynamoDbProvider;
import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Databricks;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.MySql;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Oracle;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Snowflake;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.SqlServer;
import com.scalar.db.analytics.api.request.schema.DataSourceSchema;
import com.scalar.db.analytics.datasource.scalardb.schema.ScalarDbSchemaResolver;
import com.scalar.db.analytics.service.SchemaResolution;
import com.scalar.db.analytics.service.SuppliedSchemaRejectedException;
import com.scalar.db.analytics.service.SuppliedSchemaRejectedException.Reason;
import com.scalar.db.analytics.service.datasource.NoOpSchemaResolver;
import com.scalar.db.analytics.service.datasource.ResolvedSchema;
import com.scalar.db.analytics.service.datasource.SchemaResolverException;
import com.scalar.db.analytics.service.datasource.rdbms.RdbmsSchemaResolver;
import com.scalar.db.analytics.service.datasource.rdbms.RdbmsSchemaResolverOption;
import com.scalar.db.analytics.service.datasource.rdbms.SchemaResolverDatabricks;
import com.scalar.db.analytics.service.datasource.rdbms.SchemaResolverMySql;
import com.scalar.db.analytics.service.datasource.rdbms.SchemaResolverOracle;
import com.scalar.db.analytics.service.datasource.rdbms.SchemaResolverPostgreSql;
import com.scalar.db.analytics.service.datasource.rdbms.SchemaResolverSnowflake;
import com.scalar.db.analytics.service.datasource.rdbms.SchemaResolverSqlServer;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * The server's {@link SchemaResolution}: the catalog connects to the source itself.
 *
 * <p>A provider that {@linkplain DataSourceProvider#supportsSchemaResolution() supports schema
 * resolution} is resolved over a connection the catalog opens (JDBC for the RDBMS providers,
 * ScalarDB for ScalarDB), and a supplied schema is refused for it. Any other provider requires a
 * supplied schema, which is used as is.
 */
public final class DefaultSchemaResolution implements SchemaResolution {

  @Override
  public ResolvedSchema resolve(
      UUID dataSourceId, DataSourceProvider provider, @Nullable DataSourceSchema schema)
      throws SchemaResolverException, SuppliedSchemaRejectedException {
    boolean supportsResolution = provider.supportsSchemaResolution();
    if (supportsResolution) {
      if (schema != null) {
        throw new SuppliedSchemaRejectedException(
            Reason.NOT_ALLOWED_FOR_PROVIDER,
            "Provider type '%s' resolves schemas automatically; omit manual schema."
                .formatted(provider.getType()));
      }
      SchemaResolverVisitor visitor = new SchemaResolverVisitor(dataSourceId);
      return provider.accept(visitor);
    }

    if (schema == null) {
      throw new SuppliedSchemaRejectedException(
          Reason.REQUIRED_FOR_PROVIDER,
          "Provider type '%s' requires a schema specification.".formatted(provider.getType()));
    }

    return new NoOpSchemaResolver(dataSourceId, schema).resolveSchema();
  }

  private static class SchemaResolverVisitor implements DataSourceProviderVisitor<ResolvedSchema> {
    private final UUID dataSourceId;

    private SchemaResolverVisitor(UUID dataSourceId) {
      this.dataSourceId = dataSourceId;
    }

    @Override
    public ResolvedSchema visit(MySql mySql) {
      try (RdbmsSchemaResolver resolver =
          SchemaResolverMySql.open(
              dataSourceId, mySql, RdbmsSchemaResolverOption.DEFAULT, mySql.getDatabase())) {
        return resolver.resolveSchema();
      }
    }

    @Override
    public ResolvedSchema visit(PostgreSql postgreSql) {
      try (RdbmsSchemaResolver resolver =
          SchemaResolverPostgreSql.open(
              dataSourceId, postgreSql, RdbmsSchemaResolverOption.DEFAULT)) {
        return resolver.resolveSchema();
      }
    }

    @Override
    public ResolvedSchema visit(Oracle oracle) {
      try (RdbmsSchemaResolver resolver =
          SchemaResolverOracle.open(dataSourceId, oracle, RdbmsSchemaResolverOption.DEFAULT)) {
        return resolver.resolveSchema();
      }
    }

    @Override
    public ResolvedSchema visit(SqlServer sqlServer) {
      try (RdbmsSchemaResolver resolver =
          SchemaResolverSqlServer.open(
              dataSourceId,
              sqlServer,
              RdbmsSchemaResolverOption.DEFAULT,
              sqlServer.getDatabase())) {
        return resolver.resolveSchema();
      }
    }

    @Override
    public ResolvedSchema visit(ScalarDbProvider scalarDb) {
      return ScalarDbSchemaResolver.create(dataSourceId, scalarDb).resolveSchema();
    }

    @Override
    public ResolvedSchema visit(DynamoDbProvider dynamoDb) {
      throw new IllegalStateException(
          "Manual schema providers must be handled outside SchemaResolverVisitor");
    }

    @Override
    public ResolvedSchema visit(Databricks databricks) {
      try (RdbmsSchemaResolver resolver =
          SchemaResolverDatabricks.open(
              dataSourceId,
              databricks,
              RdbmsSchemaResolverOption.DEFAULT,
              databricks.getCatalog())) {
        return resolver.resolveSchema();
      }
    }

    @Override
    public ResolvedSchema visit(Snowflake snowflake) {
      try (RdbmsSchemaResolver resolver =
          SchemaResolverSnowflake.open(
              dataSourceId,
              snowflake,
              RdbmsSchemaResolverOption.DEFAULT,
              snowflake.getDatabase())) {
        return resolver.resolveSchema();
      }
    }
  }
}
