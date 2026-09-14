/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.codec;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.api.codec.datatype.DataTypeCodec;
import com.scalar.db.analytics.api.codec.datatype.DataTypeJacksonModule;

/**
 * A factory for creating configured {@link ObjectMapper} instances for codec operations. This
 * ensures that all parts of the application use a consistent mapper configuration.
 */
public final class CodecObjectMapperFactory {

  private CodecObjectMapperFactory() {
    // prevent instantiation
  }

  /**
   * Creates a new {@link ObjectMapper} instance with the standard configuration for provider
   * codecs.
   *
   * @return a configured {@link ObjectMapper}
   */
  public static ObjectMapper create() {
    ObjectMapper mapper = new ObjectMapper();

    // Fail on unknown properties by default. This is a secure-by-default approach, especially
    // when processing JSON from potentially untrusted sources (e.g., gRPC requests).
    // If specific use cases require ignoring unknown properties (e.g., for backward compatibility
    // with trusted internal sources), a separate ObjectMapper instance should be configured
    // with DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES set to false.
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    // Enforce that primitive types cannot be null
    mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);

    // Enforce that all properties in the constructor are present in the JSON
    mapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, true);

    // Register custom deserializer for DataType interface
    // NOTE: This appears to create a circular dependency (mapper -> codec -> module -> mapper),
    // but it's actually safe because DataTypeCodec only uses the mapper for deserializing
    // concrete DataType classes (e.g., DataType.Int.class), while DataTypeJacksonModule is
    // only used for deserializing DataType interface fields in other classes (e.g., ColumnSchema).
    // However, to make this clearer and avoid confusion, consider using separate ObjectMapper
    // instances for DataTypeCodec and the returned mapper in the future.
    DataTypeCodec dataTypeCodec = new DataTypeCodec(mapper);
    DataTypeJacksonModule dataTypeModule = new DataTypeJacksonModule(dataTypeCodec);
    mapper.registerModule(dataTypeModule);

    return mapper;
  }
}
