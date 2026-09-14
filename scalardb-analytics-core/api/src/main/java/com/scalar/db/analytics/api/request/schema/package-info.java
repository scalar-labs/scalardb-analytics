/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
/**
 * Provides a simplified schema model of a data source that includes namespace, table, and column
 * schemas. For some data sources, we cannot resolve schema information automatically. For these
 * data sources, we need to require users to manually specify schema and keep it as part of the data
 * source information to construct richer schema models, that is {@link
 * com.scalar.db.analytics.api.model.Namespace}, {@link com.scalar.db.analytics.api.model.Table},
 * and {@link com.scalar.db.analytics.api.model.Column}, during the schema resolution phase. The
 * classes in this package are intended to be used for this purpose.
 */
@NullMarked
package com.scalar.db.analytics.api.request.schema;

import org.jspecify.annotations.NullMarked;
