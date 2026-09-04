/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.config;

import org.mapstruct.MapperConfig;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * Shared configuration for all MapStruct mappers. This configuration ensures consistent behavior
 * across all mappers and reduces boilerplate code.
 */
@MapperConfig(
    componentModel = "default",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MapStructConfig {}
