/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
/**
 * Type converters for transforming Java domain types to database-specific values.
 *
 * <p>This package contains Spring Data JDBC {@link
 * org.springframework.core.convert.converter.Converter} implementations that handle the
 * transformation of Java domain types to their corresponding database-specific representations
 * during write operations (INSERT/UPDATE queries).
 *
 * <p>Converters in this package handle:
 *
 * <ul>
 *   <li>{@link UuidToStringConverter} - Converts UUID objects to CHAR(36) strings
 *   <li>{@link BooleanToIntegerConverter} - Converts boolean to SMALLINT (0/1)
 *   <li>{@link BooleanToBigDecimalConverter} - Converts boolean to Oracle NUMBER(1)
 *   <li>{@link StringListToJsonConverter} - Converts List&lt;String&gt; to JSON array strings
 *   <li>{@link StringListToPostgreSqlArrayConverter} - Converts List&lt;String&gt; to PostgreSQL
 *       text[]
 * </ul>
 *
 * <p>These converters work in tandem with their reading counterparts to provide seamless
 * bidirectional type conversion between Java and database representations, ensuring data integrity
 * across different database vendors.
 */
// TODO: Move this annotation to the module level
@NullMarked
package com.scalar.db.analytics.repository.impl.spring.converter.writing;

import org.jspecify.annotations.NullMarked;
