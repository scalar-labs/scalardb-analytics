/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.model.datasource.provider;

import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import com.scalar.db.analytics.api.model.datasource.DataSourceProviderVisitor;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

@Value
@Jacksonized
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Builder(builderClassName = "Builder", buildMethodName = "build")
public class DynamoDbProvider implements DataSourceProvider {
  public static final String TYPE = "dynamodb";

  @Nullable String region;
  @Nullable String endpoint;

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public <T> T accept(DataSourceProviderVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public boolean supportsSchemaResolution() {
    return false;
  }

  // The Lombok @Builder-generated fields start as null until set, so opt this builder out of the
  // package-level @NullMarked scope rather than treating those fields as non-null.
  @NullUnmarked
  public static class Builder {
    /**
     * Builds the provider.
     *
     * @return the provider
     * @throws IllegalArgumentException if neither a region nor an endpoint is set. When the server
     *     decodes a provider from a request, this surfaces as {@code INVALID_ARGUMENT} for {@code
     *     provider_payload_json}
     */
    public DynamoDbProvider build() {
      if (region == null && endpoint == null) {
        throw new IllegalArgumentException("Either region or endpoint must be set.");
      }

      return new DynamoDbProvider(region, endpoint);
    }
  }
}
