/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.codec.datatype;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.scalar.db.analytics.api.model.DataType;
import java.io.IOException;

/**
 * Jackson deserializer for DataType that delegates to DataTypeCodec.
 *
 * <p>This deserializer enables automatic JSON deserialization of DataType fields in Spring Data
 * JDBC entities by delegating to the existing DataTypeCodec logic.
 */
public class DataTypeJsonDeserializer extends JsonDeserializer<DataType> {

  private final DataTypeCodec codec;

  public DataTypeJsonDeserializer(DataTypeCodec codec) {
    this.codec = codec;
  }

  @Override
  public DataType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    // Read the JSON node
    JsonNode node = p.getCodec().readTree(p);

    // Use DataTypeCodec to convert JSON string to DataType
    // DataTypeCodec handles the kind-based deserialization logic
    String json = node.toString();
    return codec.deserialize(json);
  }
}
