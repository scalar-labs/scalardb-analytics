/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.api.codec.CodecObjectMapperFactory;
import com.scalar.db.analytics.api.codec.schema.DataSourceSchemaCodec;
import com.scalar.db.analytics.api.request.schema.DataSourceSchema;
import com.scalar.db.analytics.grpc.mapper.config.MapStructConfig;
import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface DataSourceSchemaJsonMapper {

  DataSourceSchemaJsonMapper INSTANCE =
      org.mapstruct.factory.Mappers.getMapper(DataSourceSchemaJsonMapper.class);

  DataSourceSchemaCodec SCHEMA_CODEC = SchemaCodecHolder.CODEC;

  default @Nullable String toJson(@Nullable DataSourceSchema schema) {
    return SCHEMA_CODEC.serialize(schema);
  }

  default @Nullable DataSourceSchema fromJson(@Nullable String json) {
    return SCHEMA_CODEC.deserialize(json);
  }

  /**
   * Helper class to hold codec instance (Java 8 compatible alternative to private static method)
   */
  class SchemaCodecHolder {
    private static final DataSourceSchemaCodec CODEC = createSchemaCodec();

    private static DataSourceSchemaCodec createSchemaCodec() {
      ObjectMapper objectMapper = CodecObjectMapperFactory.create();
      return new DataSourceSchemaCodec(objectMapper);
    }
  }
}
