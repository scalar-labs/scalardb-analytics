/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest;
import com.scalar.db.analytics.service.datasource.InvalidSchemaMetadataException;
import java.util.Map;

/**
 * Maps data source registration failures to {@link AnalyticsException}, for the use cases that
 * register data sources: registering one, and initializing a catalog with some.
 */
final class DataSourceRegistrationErrors {

  private DataSourceRegistrationErrors() {}

  /**
   * Returns the {@link AnalyticsErrorCode#INVALID_SCHEMA_METADATA} error for a data source whose
   * schema contains a prohibited name.
   *
   * @param request the registration request of the data source
   * @param e the validation failure
   * @return the exception to throw
   */
  static AnalyticsException invalidSchemaMetadata(
      RegisterDataSourceRequest request, InvalidSchemaMetadataException e) {
    return new AnalyticsException(
        AnalyticsErrorCode.INVALID_SCHEMA_METADATA,
        Map.of(
            "catalog_name", request.getCatalogName(),
            "data_source_name", request.getName(),
            "field", e.getField().label(),
            "path", e.getPath(),
            "value", e.getValue()),
        e);
  }
}
