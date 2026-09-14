/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.exception;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.grpc.generated.error.v1.AnalyticsError;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Converts exceptions raised while calling the gRPC server into a unified {@link
 * AnalyticsException}.
 *
 * <p>This is the single boundary at which raw gRPC/runtime exceptions are turned into the domain
 * exception. A {@link StatusRuntimeException} is handled by inspecting its {@link AnalyticsError}
 * detail first, consulting the gRPC status only when no detail is present:
 *
 * <ul>
 *   <li><b>Structured server error, recognized code</b>: the {@link AnalyticsError} detail in
 *       {@code grpc-status-details-bin} carries a code known to this SDK. The error code and
 *       metadata are reconstructed from the detail; the gRPC status code is <em>not</em> consulted,
 *       because the domain error code is the source of truth.
 *   <li><b>Structured server error, unrecognized code</b>: the detail is present but its code is
 *       unknown (e.g. produced by a newer server). Surfaced as {@link
 *       AnalyticsErrorCode#UNRECOGNIZED_SERVER_ERROR} with the original code preserved in metadata;
 *       the gRPC status is still not consulted, because the server was reached.
 *   <li><b>Detail-less transport failure</b>: a {@link StatusRuntimeException} with no {@code
 *       AnalyticsError} detail (the server was never reached, or the error came from a proxy/mesh
 *       or the gRPC framework). The gRPC {@link Status.Code} is mapped to a client-side {@code
 *       CLIENT_ERROR} code.
 *   <li><b>Anything else</b> (a non-gRPC throwable, or a malformed detail): mapped to {@link
 *       AnalyticsErrorCode#CLIENT_INTERNAL_ERROR}.
 * </ul>
 *
 * <p>The catch-all guarantees no raw exception leaks past the SDK boundary.
 *
 * <p><b>Caller operation context.</b> The overloads taking an {@code operation} name and a {@code
 * context} map attach that caller-side context to the resulting exception's metadata, so a failure
 * still names the SDK call and the entities it acted on. This is added only on the paths where no
 * server metadata is available to identify the call — the detail-less transport failure and the
 * non-gRPC catch-all. On the detail-present paths the metadata is server-derived and authoritative,
 * so the caller context is not added.
 */
public final class GrpcExceptionMapper {

  /** Metadata key under which the failing SDK operation name is recorded. */
  public static final String OPERATION_METADATA_KEY = "operation";

  private GrpcExceptionMapper() {}

  /**
   * Converts the given throwable into an {@link AnalyticsException} with no caller context.
   *
   * @param throwable the exception raised while issuing the request or processing the response
   * @return a unified {@link AnalyticsException}
   */
  public static AnalyticsException toAnalyticsException(Throwable throwable) {
    return toAnalyticsException(throwable, Collections.emptyMap());
  }

  /**
   * Converts the given throwable into an {@link AnalyticsException}, recording the failing SDK
   * operation.
   *
   * @param throwable the exception raised while issuing the request or processing the response
   * @param operation the name of the SDK operation that failed (e.g. {@code
   *     "findDataSourceByName"})
   * @return a unified {@link AnalyticsException}
   */
  public static AnalyticsException toAnalyticsException(Throwable throwable, String operation) {
    return toAnalyticsException(throwable, operation, Collections.emptyMap());
  }

  /**
   * Converts the given throwable into an {@link AnalyticsException}, recording the failing SDK
   * operation and the entities it acted on.
   *
   * @param throwable the exception raised while issuing the request or processing the response
   * @param operation the name of the SDK operation that failed (e.g. {@code
   *     "findDataSourceByName"})
   * @param context identifiers of the entities the operation acted on (e.g. {@code catalogName})
   * @return a unified {@link AnalyticsException}
   */
  public static AnalyticsException toAnalyticsException(
      Throwable throwable, String operation, Map<String, String> context) {
    Map<String, String> callerContext = new LinkedHashMap<>();
    callerContext.put(OPERATION_METADATA_KEY, operation);
    callerContext.putAll(context);
    return toAnalyticsException(throwable, callerContext);
  }

  private static AnalyticsException toAnalyticsException(
      Throwable throwable, Map<String, String> callerContext) {
    if (throwable instanceof AnalyticsException) {
      // Already a domain exception (e.g. thrown by a nested call); pass it through unchanged.
      return (AnalyticsException) throwable;
    }
    if (throwable instanceof StatusRuntimeException) {
      StatusRuntimeException sre = (StatusRuntimeException) throwable;
      Optional<AnalyticsException> reconstructed = reconstructFromDetail(sre);
      if (reconstructed.isPresent()) {
        return reconstructed.get();
      }
      // Detail-less transport failure: no server metadata identifies the call, so surface the
      // caller context for diagnosability.
      return new AnalyticsException(mapStatusCode(sre.getStatus().getCode()), callerContext, sre);
    }
    // Non-gRPC client-side failure: likewise carries no server metadata.
    return new AnalyticsException(
        AnalyticsErrorCode.CLIENT_INTERNAL_ERROR, callerContext, throwable);
  }

  /**
   * Reconstructs an {@link AnalyticsException} from the {@link AnalyticsError} detail when one is
   * present. A recognized code yields that domain code with its metadata; an unrecognized code
   * yields {@link AnalyticsErrorCode#UNRECOGNIZED_SERVER_ERROR} (preserving the original code in
   * metadata); a malformed detail yields {@link AnalyticsErrorCode#CLIENT_INTERNAL_ERROR}. Returns
   * empty only when there is no {@link AnalyticsError} detail at all, so the caller falls back to
   * status-code mapping.
   */
  private static Optional<AnalyticsException> reconstructFromDetail(StatusRuntimeException sre) {
    com.google.rpc.Status rpcStatus = StatusProto.fromThrowable(sre);
    if (rpcStatus == null) {
      return Optional.empty();
    }
    for (Any detail : rpcStatus.getDetailsList()) {
      if (detail.is(AnalyticsError.class)) {
        try {
          AnalyticsError error = detail.unpack(AnalyticsError.class);
          Optional<AnalyticsErrorCode> code = AnalyticsErrorCode.fromCode(error.getErrorCode());
          if (code.isPresent()) {
            return Optional.of(new AnalyticsException(code.get(), error.getMetadataMap(), sre));
          }
          // The server was reached and returned a structured error, but its code is unknown to this
          // (older) SDK. Do not fall back to transport status mapping (which would misreport a
          // reachable server as a transport failure). Surface it conservatively, preserving the
          // original code for diagnostics. The category is unknown, so we do not infer retryability
          // from the gRPC status: the transport layer has already exhausted any automatic retry.
          Map<String, String> metadata = new HashMap<>(error.getMetadataMap());
          metadata.put("server_error_code", error.getErrorCode());
          return Optional.of(
              new AnalyticsException(AnalyticsErrorCode.UNRECOGNIZED_SERVER_ERROR, metadata, sre));
        } catch (InvalidProtocolBufferException e) {
          // Detail is present but cannot be parsed: a genuine client-side processing failure.
          return Optional.of(new AnalyticsException(AnalyticsErrorCode.CLIENT_INTERNAL_ERROR, sre));
        }
      }
    }
    return Optional.empty();
  }

  /** Maps a detail-less gRPC status code to a client-side {@code CLIENT_ERROR} code. */
  private static AnalyticsErrorCode mapStatusCode(Status.Code code) {
    switch (code) {
      case UNAVAILABLE:
        return AnalyticsErrorCode.SERVER_UNREACHABLE;
      case DEADLINE_EXCEEDED:
        return AnalyticsErrorCode.REQUEST_TIMEOUT;
      default:
        return AnalyticsErrorCode.CLIENT_INTERNAL_ERROR;
    }
  }
}
