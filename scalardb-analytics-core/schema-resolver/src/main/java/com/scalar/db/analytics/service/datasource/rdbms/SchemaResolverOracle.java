/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Oracle;
import java.util.Properties;
import java.util.UUID;

/**
 * This class provides the schema resolver for an Oracle data source.
 *
 * <p>This resolves all the schemas in the specified Oracle database.
 *
 * <p>The schemas of the predefined users are ignored.
 */
public class SchemaResolverOracle extends RdbmsSchemaResolver {
  private SchemaResolverOracle(
      UUID dataSourceId,
      String connectionUrl,
      Properties connectionProperties,
      RdbmsSchemaResolverOption option) {
    super(
        dataSourceId,
        connectionUrl,
        connectionProperties,
        option,
        new SchemaResolverStrategyOracle());
  }

  /**
   * Constructs a schema resolver for an Oracle data source. This also opens a JDBC connection to
   * the data source. It is user's responsibility to close the connection after using the schema
   * resolver by calling the {@code close()} method.
   *
   * @param dataSourceId the ID of the data source
   * @param oracle the Oracle data source information
   * @param option the option for the schema resolver
   */
  public static SchemaResolverOracle open(
      UUID dataSourceId, Oracle oracle, RdbmsSchemaResolverOption option) {
    return open(dataSourceId, oracle.getUrl(), oracle.getProperties(), option);
  }

  /**
   * Constructs a schema resolver for an Oracle data source. This also opens a JDBC connection to
   * the data source. It is user's responsibility to close the connection after using the schema
   * resolver by calling the {@code close()} method.
   *
   * @param dataSourceId the ID of the data source
   * @param connectionUrl the JDBC connection URL
   * @param connectionProperties the JDBC connection properties
   * @param option the option for the schema resolver
   */
  public static SchemaResolverOracle open(
      UUID dataSourceId,
      String connectionUrl,
      Properties connectionProperties,
      RdbmsSchemaResolverOption option) {
    SchemaResolverOracle resolver =
        new SchemaResolverOracle(dataSourceId, connectionUrl, connectionProperties, option);
    resolver.openConnection();
    return resolver;
  }
}
