/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
/**
 * Type converters for reading database values and transforming them to Java domain types.
 *
 * <p>This package contains Spring Data JDBC {@link
 * org.springframework.core.convert.converter.Converter} implementations that handle the
 * transformation of database-specific column values to their corresponding Java types during read
 * operations (SELECT queries).
 *
 * <p>Converters in this package handle:
 *
 * <ul>
 *   <li>{@link StringToUuidConverter} - Converts CHAR(36) strings to UUID objects
 *   <li>{@link IntegerToBooleanConverter} - Converts SMALLINT (0/1) to boolean
 *   <li>{@link BigDecimalToBooleanConverter} - Converts Oracle NUMBER(1) to boolean
 *   <li>{@link JsonToStringListConverter} - Converts JSON arrays to List&lt;String&gt;
 *   <li>{@link PostgreSqlArrayToStringListConverter} - Converts PostgreSQL text[] to
 *       List&lt;String&gt;
 *   <li>{@link H2ArrayToStringListConverter} - Converts H2 Object[] arrays to List&lt;String&gt;
 *   <li>{@link PGobjectToStringConverter} - Converts PostgreSQL PGobject (JSONB) to String
 * </ul>
 *
 * <p>These converters ensure that database-specific representations are correctly transformed into
 * the expected Java types used in the domain model, abstracting away database differences.
 */
// TODO: Move this annotation to the module level
@NullMarked
package com.scalar.db.analytics.repository.impl.spring.converter.reading;

import org.jspecify.annotations.NullMarked;
