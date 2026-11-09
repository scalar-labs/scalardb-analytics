/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.scalar.db.analytics.api.codec.provider.ProviderCodecException;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.grpc.generated.catalog.v1.InitializeCatalogRequest;
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

  /**
   * Returns whether a {@link ProviderCodecException} raised while decoding caller-supplied provider
   * JSON means that the JSON was rejected, rather than that the codec failed.
   *
   * <p>The provider codecs bind the JSON with Jackson, which reports what it rejects as a {@link
   * JsonProcessingException}: a value of the wrong type, an unknown or missing property, or an
   * argument check of the provider's builder (e.g. a DynamoDB provider without a region or an
   * endpoint), which Jackson wraps. Any other cause, such as a {@link NullPointerException} thrown
   * by a codec, is a server-side defect.
   */
  static boolean isRejectedInput(ProviderCodecException e) {
    return e.getCause() instanceof JsonProcessingException;
  }

  /** The request context reported in the metadata of a decoding error. */
  static final class Context {
    private final String catalogName;
    private final String dataSourceName;
    private final @Nullable Integer dataSourceIndex;

    private Context(String catalogName, String dataSourceName, @Nullable Integer dataSourceIndex) {
      this.catalogName = catalogName;
      this.dataSourceName = dataSourceName;
      this.dataSourceIndex = dataSourceIndex;
    }

    /** Returns the name of the catalog that the data source is registered in. */
    String catalogName() {
      return catalogName;
    }

    /** The context of a data source registration request. */
    static Context of(RegisterRequest request) {
      return new Context(request.getCatalogName(), request.getDataSourceName(), null);
    }

    /**
     * The context of the {@code index}-th entry (0-based) of {@code data_sources} in a catalog
     * initialization request. The catalog is the one of the outer request, which is the catalog the
     * caller initializes; the {@code catalog_name} of the entry is ignored.
     */
    static Context of(InitializeCatalogRequest request, int index) {
      return new Context(
          request.getCatalogName(), request.getDataSources(index).getDataSourceName(), index);
    }
  }

  static AnalyticsException invalidField(
      Context context, String field, String reason, @Nullable Throwable cause) {
    Map<String, String> metadata = new LinkedHashMap<>();
    metadata.put("catalog_name", context.catalogName);
    metadata.put("data_source_name", context.dataSourceName);
    if (context.dataSourceIndex != null) {
      metadata.put("data_source_index", String.valueOf(context.dataSourceIndex));
    }
    metadata.put("field", field);
    metadata.put("reason", reason);
    return cause == null
        ? new AnalyticsException(AnalyticsErrorCode.INVALID_ARGUMENT, metadata)
        : new AnalyticsException(AnalyticsErrorCode.INVALID_ARGUMENT, metadata, cause);
  }
}
