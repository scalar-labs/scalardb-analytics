/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.codec.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import com.scalar.db.analytics.api.model.datasource.provider.DynamoDbProvider;
import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Databricks;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.MySql;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Oracle;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Snowflake;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.SqlServer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Registry for managing provider-specific codecs. Maps provider types and classes to their
 * corresponding codecs. Future plugin support can be added by extending the registration mechanism.
 */
public class ProviderCodecRegistry {

  private final Map<String, ProviderCodec<?>> codecsByType = new HashMap<>();
  private final Map<Class<? extends DataSourceProvider>, ProviderCodec<?>> codecsByClass =
      new HashMap<>();

  private ProviderCodecRegistry(ObjectMapper objectMapper) {
    // Create and register codec instances
    register(MySql.TYPE, MySql.class, new MySqlCodec(objectMapper));
    register(PostgreSql.TYPE, PostgreSql.class, new PostgreSqlCodec(objectMapper));
    register(Oracle.TYPE, Oracle.class, new OracleCodec(objectMapper));
    register(SqlServer.TYPE, SqlServer.class, new SqlServerCodec(objectMapper));
    register(Databricks.TYPE, Databricks.class, new DatabricksCodec(objectMapper));
    register(Snowflake.TYPE, Snowflake.class, new SnowflakeCodec(objectMapper));
    register(
        ScalarDbProvider.TYPE, ScalarDbProvider.class, new ScalarDbProviderCodec(objectMapper));
    register(
        DynamoDbProvider.TYPE, DynamoDbProvider.class, new DynamoDbProviderCodec(objectMapper));
  }

  private <T extends DataSourceProvider> void register(
      String type, Class<T> providerClass, ProviderCodec<T> codec) {
    String normalizedType = normalize(type);
    codecsByType.put(normalizedType, codec);
    codecsByClass.put(providerClass, codec);
  }

  /**
   * Creates a new ProviderCodecRegistry instance with all standard provider codecs.
   *
   * @param objectMapper the Jackson ObjectMapper to use for serialization/deserialization
   * @return a new ProviderCodecRegistry instance
   */
  public static ProviderCodecRegistry create(ObjectMapper objectMapper) {
    return new ProviderCodecRegistry(objectMapper);
  }

  /**
   * Gets the codec for the specified provider type.
   *
   * @param providerType the type of provider
   * @return the codec for the provider type
   * @throws IllegalArgumentException if no codec is registered for the type
   */
  public ProviderCodec<?> getCodecForType(String providerType) {
    ProviderCodec<?> codec = codecsByType.get(normalize(providerType));
    if (codec == null) {
      throw new IllegalArgumentException("No codec registered for provider type: " + providerType);
    }
    return codec;
  }

  /**
   * Gets the codec for the specified provider class.
   *
   * @param providerClass the class of provider
   * @return the codec for the provider class
   * @throws IllegalArgumentException if no codec is registered for the class
   */
  @SuppressWarnings("unchecked")
  public <T extends DataSourceProvider> ProviderCodec<T> getCodecForClass(Class<T> providerClass) {
    ProviderCodec<T> codec = (ProviderCodec<T>) codecsByClass.get(providerClass);
    if (codec == null) {
      throw new IllegalArgumentException(
          "No codec registered for provider class: " + providerClass);
    }
    return codec;
  }

  private static String normalize(String type) {
    if (type == null || type.trim().isEmpty()) {
      throw new IllegalArgumentException("Provider type must not be null or empty");
    }
    return type.toLowerCase(Locale.ROOT);
  }
}
