/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
/**
 * Spring Data JDBC type converters for handling database-specific type mappings.
 *
 * <p>This package contains converters that bridge the gap between Java domain types and
 * database-specific column types. Since different databases have varying support for types like
 * UUID, boolean, and arrays, these converters ensure proper data transformation during read and
 * write operations.
 *
 * <p>The converters are organized into two sub-packages:
 *
 * <ul>
 *   <li>{@code reading} - Converters for transforming database values to Java types during reads
 *   <li>{@code writing} - Converters for transforming Java types to database values during writes
 * </ul>
 *
 * <p>Key type conversions handled:
 *
 * <ul>
 *   <li><b>UUID</b> - Native UUID (PostgreSQL, H2) vs CHAR(36) (Oracle, SQL Server, MySQL)
 *   <li><b>Boolean</b> - Native BOOLEAN (PostgreSQL, H2) vs SMALLINT/BIT (others)
 *   <li><b>String Arrays</b> - Native arrays (PostgreSQL, H2) vs JSON (Oracle, SQL Server, MySQL)
 * </ul>
 *
 * <p>These converters are registered in {@link
 * com.scalar.db.analytics.repository.impl.spring.config.SpringDataJdbcConfiguration} based on the
 * detected database type, ensuring optimal performance and compatibility.
 */
// TODO: Move this annotation to the module level
@NullMarked
package com.scalar.db.analytics.repository.impl.spring.converter;

import org.jspecify.annotations.NullMarked;
