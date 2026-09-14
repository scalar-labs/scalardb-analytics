/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.api.codec.CodecObjectMapperFactory;
import com.scalar.db.analytics.api.codec.provider.DataSourceProviderCodec;
import com.scalar.db.analytics.api.codec.provider.ProviderCodecRegistry;
import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import com.scalar.db.analytics.grpc.mapper.config.MapStructConfig;
import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

/**
 * Helper mapper for converting between {@link DataSourceProvider} implementations and the JSON
 * representation (provider type + provider-specific fields) used on the gRPC transport layer.
 */
@Mapper(config = MapStructConfig.class)
public interface DataSourceProviderJsonMapper {

  DataSourceProviderJsonMapper INSTANCE =
      org.mapstruct.factory.Mappers.getMapper(DataSourceProviderJsonMapper.class);

  DataSourceProviderCodec PROVIDER_CODEC = ProviderCodecHolder.INSTANCE;

  @Named("providerToJson")
  default String toJson(DataSourceProvider provider) {
    return PROVIDER_CODEC.serialize(provider);
  }

  @Named("providerFromJson")
  default @Nullable DataSourceProvider fromJson(@Nullable String json) {
    if (json == null || json.trim().isEmpty()) {
      return null;
    }
    return PROVIDER_CODEC.deserialize(json);
  }

  /** Helper class to hold the provider codec instance (Java 8 compatible). */
  final class ProviderCodecHolder {
    static final DataSourceProviderCodec INSTANCE = createProviderCodec();

    private static DataSourceProviderCodec createProviderCodec() {
      ObjectMapper objectMapper = CodecObjectMapperFactory.create();
      ProviderCodecRegistry registry = ProviderCodecRegistry.create(objectMapper);
      return new DataSourceProviderCodec(registry, objectMapper);
    }

    private ProviderCodecHolder() {
      // Utility class
    }
  }
}
