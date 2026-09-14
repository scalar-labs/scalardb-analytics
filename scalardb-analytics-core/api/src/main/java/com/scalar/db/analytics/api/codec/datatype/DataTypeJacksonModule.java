/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.codec.datatype;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.scalar.db.analytics.api.model.DataType;

/**
 * Jackson module that registers custom deserializer for DataType.
 *
 * <p>This module enables CodecObjectMapperFactory's ObjectMapper to automatically handle DataType
 * fields in JSON deserialization by delegating to DataTypeCodec.
 *
 * <p>Usage: Register this module with CodecObjectMapperFactory's ObjectMapper during
 * initialization.
 */
public class DataTypeJacksonModule extends SimpleModule {

  private static final long serialVersionUID = -2912709147279128039L;

  public DataTypeJacksonModule(DataTypeCodec codec) {
    super("DataTypeModule");
    // Register custom deserializer for DataType interface
    // Deserializer delegates to DataTypeCodec for kind-based deserialization
    addDeserializer(DataType.class, new DataTypeJsonDeserializer(codec));
  }
}
