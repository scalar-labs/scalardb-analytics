/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper;

import com.scalar.db.analytics.api.codec.provider.ProviderCodecException;
import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest;
import com.scalar.db.analytics.api.request.schema.DataSourceSchema;
import com.scalar.db.analytics.grpc.generated.datasource.v1.RegisterRequest;
import com.scalar.db.analytics.grpc.mapper.datasource.DataSourceProviderJsonMapper;
import com.scalar.db.analytics.grpc.mapper.datasource.DataSourceSchemaJsonMapper;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/** Decodes the {@link RegisterRequest}s that the server receives, for {@link RequestMapper}. */
final class RegisterRequestDecoder {

  private RegisterRequestDecoder() {}

  /** Decodes a data source registration request, reporting errors with the given context. */
  static RegisterDataSourceRequest decode(
      RegisterRequest request, RegisterRequestErrors.Context context) {
    return decode(request, context, DataSourceProviderJsonMapper.INSTANCE::fromJson);
  }

  /**
   * Decodes a data source registration request with the given provider decoder. Tests use it to
   * stand in for a provider codec that fails.
   */
  static RegisterDataSourceRequest decode(
      RegisterRequest request,
      RegisterRequestErrors.Context context,
      Function<String, @Nullable DataSourceProvider> providerDecoder) {
    @Nullable DataSourceProvider provider;
    try {
      provider = providerDecoder.apply(request.getProviderPayloadJson());
    } catch (IllegalArgumentException e) {
      // The codecs throw IllegalArgumentException for malformed JSON, a missing or unknown provider
      // type, and a payload that is not an object.
      throw RegisterRequestErrors.invalidField(
          context, RegisterRequestErrors.PROVIDER_PAYLOAD_JSON, RegisterRequestErrors.INVALID, e);
    } catch (ProviderCodecException e) {
      if (!RegisterRequestErrors.isRejectedInput(e)) {
        throw e;
      }
      throw RegisterRequestErrors.invalidField(
          context, RegisterRequestErrors.PROVIDER_PAYLOAD_JSON, RegisterRequestErrors.INVALID, e);
    }
    if (provider == null) {
      throw RegisterRequestErrors.invalidField(
          context,
          RegisterRequestErrors.PROVIDER_PAYLOAD_JSON,
          RegisterRequestErrors.REQUIRED,
          null);
    }

    DataSourceSchema schema;
    try {
      schema =
          DataSourceSchemaJsonMapper.INSTANCE.fromJson(
              request.hasSchemaJson() ? request.getSchemaJson() : null);
    } catch (IllegalArgumentException e) {
      throw RegisterRequestErrors.invalidField(
          context, RegisterRequestErrors.SCHEMA_JSON, RegisterRequestErrors.INVALID, e);
    }

    return new RegisterDataSourceRequest(
        context.catalogName(), request.getDataSourceName(), provider, schema);
  }
}
