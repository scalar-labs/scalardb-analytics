/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

/**
 * Meta-annotation to indicate that a MapStruct mapping method should ignore protobuf default
 * fields. This is used to avoid mapping the ByteString fields that protobuf generates for string
 * fields (e.g., nameBytes for a field called name).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@Mappings({
  @Mapping(target = "unknownFields", ignore = true),
  @Mapping(target = "memoizedHashCode", ignore = true),
  @Mapping(target = "memoizedIsInitialized", ignore = true),
  @Mapping(target = "memoizedSerializedSize", ignore = true),
  @Mapping(target = "parserForType", ignore = true),
  @Mapping(target = "defaultInstanceForType", ignore = true),
  @Mapping(target = "allFields", ignore = true),
  @Mapping(target = "descriptorForType", ignore = true),
  @Mapping(target = "initializationErrorString", ignore = true),
  @Mapping(target = "initialized", ignore = true),
  @Mapping(target = "serializedSize", ignore = true)
})
public @interface IgnoreProtobufDefaults {}
