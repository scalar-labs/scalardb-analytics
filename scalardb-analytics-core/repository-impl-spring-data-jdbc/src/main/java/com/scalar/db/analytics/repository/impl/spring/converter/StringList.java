/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.converter;

import java.util.List;

/**
 * A marker wrapper for {@code List<String>} to avoid Spring Data JDBC's automatic array handling.
 * This allows us to use custom converters for database-specific storage.
 */
public record StringList(List<String> values) {}
