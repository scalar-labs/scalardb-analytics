/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.datatype;

import com.scalar.db.analytics.api.codec.CodecObjectMapperFactory;
import com.scalar.db.analytics.api.codec.datatype.DataTypeCodec;
import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.grpc.mapper.config.MapStructConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

/**
 * Mapper for converting between DataType domain objects and JSON representation.
 *
 * <p>This mapper uses {@link DataTypeCodec} to handle the serialization and deserialization of
 * {@link DataType} instances to/from JSON strings. The JSON format includes a {@code kind} field
 * that identifies the data type, plus any type-specific fields.
 */
@Mapper(config = MapStructConfig.class)
public interface DataTypeJsonMapper {

  DataTypeJsonMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(DataTypeJsonMapper.class);

  DataTypeCodec DATA_TYPE_CODEC = new DataTypeCodec(CodecObjectMapperFactory.create());

  /**
   * Converts a DataType domain object to its JSON string representation.
   *
   * @param dataType the data type to convert
   * @return JSON string representation
   * @throws IllegalArgumentException if dataType is null
   */
  @Named("dataTypeToJson")
  default String toJson(DataType dataType) {
    return DATA_TYPE_CODEC.serialize(dataType);
  }

  /**
   * Converts a JSON string to a DataType domain object.
   *
   * @param json the JSON string
   * @return DataType instance
   * @throws IllegalArgumentException if json is null or empty
   */
  @Named("jsonToDataType")
  default DataType fromJson(String json) {
    return DATA_TYPE_CODEC.deserialize(json);
  }
}
