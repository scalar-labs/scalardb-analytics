/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import com.google.protobuf.ProtocolMessageEnum;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Decodes request fields whose wire form is wider than the domain value: IDs sent as strings and
 * proto enums that admit an unspecified or unrecognized value.
 *
 * <p>A field that does not decode fails with {@link AnalyticsErrorCode#INVALID_ARGUMENT} carrying
 * {@code field} (the proto field name) and {@code reason} in its metadata, the same convention as
 * use-case validation. Decoding happens here, at the server boundary, rather than in the MapStruct
 * mappers: their enum mappings throw {@link IllegalArgumentException}, and {@code
 * PasswordBackendMapper} in the {@code grpc} module is also used by the client SDK on responses.
 */
final class RequestFields {

  /** The ID is not a UUID. */
  static final String MALFORMED_UUID = "malformed_uuid";

  /** The enum is left at its zero value. */
  static final String UNSPECIFIED = "unspecified";

  /** The enum has a value this server does not know, e.g. one sent by a newer client. */
  static final String UNRECOGNIZED = "unrecognized";

  private RequestFields() {}

  /**
   * Parses an ID field.
   *
   * @param field the proto field name, e.g. {@code role_id}
   * @param value the field value
   * @return the ID
   * @throws AnalyticsException {@link AnalyticsErrorCode#INVALID_ARGUMENT} if the value is not a
   *     UUID
   */
  static UUID uuid(String field, String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      throw invalidArgument(field, MALFORMED_UUID, e);
    }
  }

  /**
   * Requires a proto enum field to hold a concrete value.
   *
   * <p>Relies on the proto3 conventions that the zero value is the {@code *_UNSPECIFIED}
   * placeholder and that protoc generates {@code UNRECOGNIZED} for unknown wire values.
   *
   * @param field the proto field name, e.g. {@code grantee_type}
   * @param value the field value
   * @return {@code value}
   * @throws AnalyticsException {@link AnalyticsErrorCode#INVALID_ARGUMENT} if the value is the
   *     unspecified placeholder or unrecognized
   */
  static <E extends Enum<E> & ProtocolMessageEnum> E specified(String field, E value) {
    // getNumber() throws on UNRECOGNIZED, so check it first.
    if ("UNRECOGNIZED".equals(value.name())) {
      throw invalidArgument(field, UNRECOGNIZED, null);
    }
    if (value.getNumber() == 0) {
      throw invalidArgument(field, UNSPECIFIED, null);
    }
    return value;
  }

  private static AnalyticsException invalidArgument(
      String field, String reason, @Nullable Throwable cause) {
    Map<String, String> metadata = Map.of("field", field, "reason", reason);
    return cause == null
        ? new AnalyticsException(AnalyticsErrorCode.INVALID_ARGUMENT, metadata)
        : new AnalyticsException(AnalyticsErrorCode.INVALID_ARGUMENT, metadata, cause);
  }
}
