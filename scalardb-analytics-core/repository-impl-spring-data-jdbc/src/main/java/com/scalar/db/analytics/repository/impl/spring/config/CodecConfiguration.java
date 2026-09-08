/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.api.codec.datatype.DataTypeCodec;
import com.scalar.db.analytics.api.codec.provider.DataSourceProviderCodec;
import com.scalar.db.analytics.api.codec.provider.ProviderCodecRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for provider codec beans. Creates codec instances using the factory methods
 * from the core module to avoid Spring dependencies in core.
 */
@Configuration
public class CodecConfiguration {

  /**
   * Creates a ProviderCodecRegistry bean.
   *
   * @param objectMapper the Jackson ObjectMapper bean
   * @return a configured ProviderCodecRegistry
   */
  @Bean
  public ProviderCodecRegistry providerCodecRegistry(ObjectMapper objectMapper) {
    return ProviderCodecRegistry.create(objectMapper);
  }

  /**
   * Creates a DataSourceProviderCodec bean.
   *
   * @param registry the ProviderCodecRegistry bean
   * @param objectMapper the Jackson ObjectMapper bean
   * @return a configured DataSourceProviderCodec
   */
  @Bean
  public DataSourceProviderCodec dataSourceProviderCodec(
      ProviderCodecRegistry registry, ObjectMapper objectMapper) {
    return new DataSourceProviderCodec(registry, objectMapper);
  }

  /**
   * Create a DataTypeCodec bean.
   *
   * @param objectMapper the Jackson ObjectMapper bean
   * @return a configured DataTypeCodec
   */
  @Bean
  public DataTypeCodec dataTypeCodec(ObjectMapper objectMapper) {
    return new DataTypeCodec(objectMapper);
  }
}
