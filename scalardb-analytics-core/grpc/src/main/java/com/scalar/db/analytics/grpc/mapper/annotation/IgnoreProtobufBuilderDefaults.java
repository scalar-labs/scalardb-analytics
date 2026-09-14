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
 * Meta-annotation to indicate that a MapStruct mapping method should ignore protobuf Builder
 * default fields and methods. This is used when mapping to protobuf Builder instances.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@Mappings({
  // Common Builder fields
  @Mapping(target = "unknownFields", ignore = true),
  @Mapping(target = "allFields", ignore = true),
  // Builder methods
  @Mapping(target = "mergeFrom", ignore = true),
  @Mapping(target = "clearField", ignore = true),
  @Mapping(target = "clearOneof", ignore = true),
  @Mapping(target = "mergeUnknownFields", ignore = true)
})
public @interface IgnoreProtobufBuilderDefaults {}
