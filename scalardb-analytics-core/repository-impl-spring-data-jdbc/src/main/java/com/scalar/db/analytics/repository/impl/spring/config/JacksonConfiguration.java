/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.api.codec.CodecObjectMapperFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Jackson ObjectMapper used in provider codec operations. Provides a shared,
 * optimized ObjectMapper instance for better performance.
 */
@Configuration
public class JacksonConfiguration {

  /**
   * Creates a shared ObjectMapper instance optimized for provider codec operations. This mapper is
   * configured to handle provider JSON serialization/deserialization efficiently.
   *
   * @return configured ObjectMapper instance
   */
  @Bean
  public ObjectMapper objectMapper() {
    return CodecObjectMapperFactory.create();
  }
}
