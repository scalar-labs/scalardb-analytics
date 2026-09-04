/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.codec.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.api.codec.CodecObjectMapperFactory;
import com.scalar.db.analytics.api.model.datasource.provider.DynamoDbProvider;
import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Databricks;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.MySql;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Oracle;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Snowflake;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.SqlServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Property-based tests for {@link ProviderCodecRegistry}. */
class ProviderCodecRegistryPropertyTest {

  private ProviderCodecRegistry registry;

  @BeforeEach
  void setUp() {
    ObjectMapper objectMapper = CodecObjectMapperFactory.create();
    registry = ProviderCodecRegistry.create(objectMapper);
  }

  @Test
  void create_shouldRegisterAllStandardProviders() {
    // Verify all standard provider types are registered
    assertThat(registry.getCodecForType(MySql.TYPE)).isNotNull();
    assertThat(registry.getCodecForType(PostgreSql.TYPE)).isNotNull();
    assertThat(registry.getCodecForType(Oracle.TYPE)).isNotNull();
    assertThat(registry.getCodecForType(SqlServer.TYPE)).isNotNull();
    assertThat(registry.getCodecForType(Databricks.TYPE)).isNotNull();
    assertThat(registry.getCodecForType(Snowflake.TYPE)).isNotNull();
    assertThat(registry.getCodecForType(ScalarDbProvider.TYPE)).isNotNull();
    assertThat(registry.getCodecForType(DynamoDbProvider.TYPE)).isNotNull();
  }

  @Test
  void getCodecForClass_shouldReturnCorrectCodec() {
    // Verify class-based codec lookup
    assertThat(registry.getCodecForClass(MySql.class)).isNotNull();
    assertThat(registry.getCodecForClass(PostgreSql.class)).isNotNull();
    assertThat(registry.getCodecForClass(Oracle.class)).isNotNull();
    assertThat(registry.getCodecForClass(SqlServer.class)).isNotNull();
    assertThat(registry.getCodecForClass(Databricks.class)).isNotNull();
    assertThat(registry.getCodecForClass(Snowflake.class)).isNotNull();
    assertThat(registry.getCodecForClass(ScalarDbProvider.class)).isNotNull();
    assertThat(registry.getCodecForClass(DynamoDbProvider.class)).isNotNull();
  }

  @Test
  void getCodecForType_shouldReturnDifferentCodecsForDifferentTypes() {
    ProviderCodec<?> mysqlCodec = registry.getCodecForType(MySql.TYPE);
    ProviderCodec<?> postgresCodec = registry.getCodecForType(PostgreSql.TYPE);

    assertThat(mysqlCodec).isNotNull();
    assertThat(postgresCodec).isNotNull();
    assertThat(mysqlCodec).isNotSameAs(postgresCodec);
  }

  @Test
  void getCodecForClass_shouldReturnDifferentCodecsForDifferentClasses() {
    ProviderCodec<?> mysqlCodec = registry.getCodecForClass(MySql.class);
    ProviderCodec<?> postgresCodec = registry.getCodecForClass(PostgreSql.class);

    assertThat(mysqlCodec).isNotNull();
    assertThat(postgresCodec).isNotNull();
    assertThat(mysqlCodec).isNotSameAs(postgresCodec);
  }
}
