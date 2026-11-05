/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.grpc.generated.datasource.v1.RegisterRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Errors for a {@link RegisterRequest} that {@link RequestMapper} cannot decode. */
final class RegisterRequestErrors {

  /** Field name of the provider JSON in {@link RegisterRequest}. */
  static final String PROVIDER_PAYLOAD_JSON = "provider_payload_json";

  /** Field name of the schema JSON in {@link RegisterRequest}. */
  static final String SCHEMA_JSON = "schema_json";

  /** Reason: the field is empty but required. */
  static final String REQUIRED = "required";

  /** Reason: the field does not decode, or decodes to a value its type rejects. */
  static final String INVALID = "invalid";

  private RegisterRequestErrors() {}

  static AnalyticsException invalidField(
      RegisterRequest request, String field, String reason, @Nullable Throwable cause) {
    Map<String, String> metadata = new LinkedHashMap<>();
    metadata.put("catalog_name", request.getCatalogName());
    metadata.put("data_source_name", request.getDataSourceName());
    metadata.put("field", field);
    metadata.put("reason", reason);
    return cause == null
        ? new AnalyticsException(AnalyticsErrorCode.INVALID_ARGUMENT, metadata)
        : new AnalyticsException(AnalyticsErrorCode.INVALID_ARGUMENT, metadata, cause);
  }
}
