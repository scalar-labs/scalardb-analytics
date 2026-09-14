/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import com.google.protobuf.Any;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.grpc.generated.error.v1.AnalyticsError;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A gRPC server interceptor that provides centralized exception handling for all service methods.
 * This interceptor catches exceptions thrown by service implementations and maps them to
 * appropriate gRPC status codes.
 *
 * <p>Error details are transmitted via the {@code grpc-status-details-bin} trailing metadata, which
 * carries a {@code google.rpc.Status} with an {@code AnalyticsError} detail for structured access.
 *
 * <p>See ERROR_MAPPING.md for the complete error mapping documentation.
 */
public class ExceptionHandlingInterceptor implements ServerInterceptor {
  private static final Logger logger = LoggerFactory.getLogger(ExceptionHandlingInterceptor.class);

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

    // next.startCall() synchronously invokes inner interceptors (e.g., AuthenticationInterceptor).
    // Exceptions thrown during interceptor setup must be caught here because they occur before
    // the listener callbacks (onHalfClose, onMessage) are wired up.
    ServerCall.Listener<ReqT> listener;
    try {
      listener = next.startCall(call, headers);
    } catch (Exception e) {
      handleException(e, call);
      return new ServerCall.Listener<>() {};
    }

    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(listener) {

      @Override
      public void onHalfClose() {
        try {
          super.onHalfClose();
        } catch (Exception e) {
          handleException(e, call);
        }
      }

      @Override
      public void onMessage(ReqT message) {
        try {
          super.onMessage(message);
        } catch (Exception e) {
          handleException(e, call);
        }
      }
    };
  }

  <ReqT, RespT> void handleException(Exception exception, ServerCall<ReqT, RespT> call) {
    AnalyticsErrorCode errorCode;
    Status status;
    AnalyticsError analyticsError;

    if (exception instanceof AnalyticsException ae) {
      errorCode = ae.getErrorCode();
      status = mapToGrpcStatus(errorCode).withDescription(ae.getMessage());
      analyticsError =
          AnalyticsError.newBuilder()
              .setErrorCode(errorCode.getCode())
              .putAllMetadata(ae.getMetadata())
              .build();
    } else if (exception instanceof IllegalArgumentException) {
      // TODO(#499): Temporary bridge. Request-path validation / "not found" code still throws bare
      // IllegalArgumentException; map it to INVALID_ARGUMENT so client input errors are not
      // mislabeled as INTERNAL. Remove this branch once those sites throw AnalyticsException.
      errorCode = AnalyticsErrorCode.INVALID_ARGUMENT;
      status =
          Status.INVALID_ARGUMENT.withDescription(
              errorCode.getCode() + ": " + exception.getMessage());
      analyticsError = AnalyticsError.newBuilder().setErrorCode(errorCode.getCode()).build();
    } else {
      errorCode = AnalyticsErrorCode.INTERNAL_ERROR;
      status = Status.INTERNAL.withDescription(errorCode.getCode() + ": " + exception.getMessage());
      analyticsError = AnalyticsError.newBuilder().setErrorCode(errorCode.getCode()).build();
    }

    // Log at appropriate level based on error category
    if (errorCode.isUserError()) {
      logger.warn("gRPC client error: {}", status, exception);
    } else {
      logger.error("gRPC server error: {}", status, exception);
    }

    StatusRuntimeException structured = toStructuredException(status, analyticsError);
    call.close(structured.getStatus(), structured.getTrailers());
  }

  /**
   * Wraps the error in a {@link StatusRuntimeException} that carries the structured {@link
   * AnalyticsError} to the client, not just the human-readable message.
   *
   * <p>This uses the gRPC "rich error model": a {@code google.rpc.Status} (gRPC status code +
   * message + {@code Any}-packed details) is serialized into the binary {@code
   * grpc-status-details-bin} response trailer. {@link StatusProto#toStatusRuntimeException} is the
   * helper that performs that serialization and produces the exception whose trailers carry the
   * payload; the SDK reverses it with {@code StatusProto.fromThrowable(...)} to recover the error
   * code and metadata.
   */
  private StatusRuntimeException toStructuredException(
      Status status, AnalyticsError analyticsError) {
    com.google.rpc.Status rpcStatus =
        com.google.rpc.Status.newBuilder()
            .setCode(status.getCode().value())
            .setMessage(status.getDescription() != null ? status.getDescription() : "")
            .addDetails(Any.pack(analyticsError))
            .build();

    return StatusProto.toStatusRuntimeException(rpcStatus);
  }

  /**
   * Maps an {@link AnalyticsErrorCode} to a gRPC {@link Status}.
   *
   * @param errorCode the error code
   * @return the corresponding gRPC status (without description)
   */
  static Status mapToGrpcStatus(AnalyticsErrorCode errorCode) {
    // Retryability is a domain property (AnalyticsErrorCode), independent of gRPC. Consume it here
    // so
    // every retryable code surfaces as UNAVAILABLE — a gRPC-retryable status that the transport
    // retries with backoff. This is the single consumer of isRetryable() and keeps the category and
    // gRPC-status axes from drifting: a code declared retryable cannot accidentally map to a
    // non-retryable status.
    if (errorCode.isRetryable()) {
      return Status.UNAVAILABLE;
    }
    // Non-retryable codes get an explicit, semantically-honest status. The switch is exhaustive so
    // a
    // newly added code forces a mapping decision here.
    return switch (errorCode) {
      case AUTHENTICATION_FAILED, TOKEN_EXPIRED, TOKEN_INVALID, SCALARDB_BACKEND_TOKEN_EXPIRED ->
          Status.UNAUTHENTICATED;
      case ACCESS_DENIED -> Status.PERMISSION_DENIED;
      case CATALOG_ALREADY_EXISTS,
              DATA_SOURCE_ALREADY_EXISTS,
              NAMESPACE_ALREADY_EXISTS,
              TABLE_ALREADY_EXISTS,
              ROLE_ALREADY_EXISTS,
              USER_ALREADY_EXISTS,
              ROLE_ALREADY_ASSIGNED,
              PERMISSION_ALREADY_GRANTED ->
          Status.ALREADY_EXISTS;
      case CATALOG_NOT_EMPTY,
              DATA_SOURCE_NOT_EMPTY,
              USER_NOT_EMPTY,
              BUILT_IN_ENTITY_NOT_MODIFIABLE ->
          Status.FAILED_PRECONDITION;
      case DATA_SOURCE_AUTHENTICATION_FAILED, DATA_SOURCE_UNREACHABLE, INVALID_ARGUMENT ->
          Status.INVALID_ARGUMENT;
      case ANALYTICS_DB_OPERATION_FAILED, SCALARDB_PRIVILEGE_CHECK_FAILURE, INTERNAL_ERROR ->
          Status.INTERNAL;
      case DATA_INCONSISTENCY -> Status.DATA_LOSS;
      // Retryable codes are handled by the isRetryable() branch above; listed to keep the switch
      // exhaustive and to fail loudly if their category ever changes without a remapping.
      case ANALYTICS_DB_CONNECTION_FAILED, SCALARDB_CLUSTER_UNAVAILABLE ->
          throw new IllegalStateException(
              "Retryable code must be mapped by the isRetryable() branch: " + errorCode.getCode());
      // Client-side codes are produced only by the SDK when no structured server error is
      // available;
      // the server must never emit them. Listed to keep this switch exhaustive.
      case SERVER_UNREACHABLE, REQUEST_TIMEOUT, CLIENT_INTERNAL_ERROR, UNRECOGNIZED_SERVER_ERROR ->
          throw new IllegalStateException(
              "Client-side error code must not be produced by the server: " + errorCode.getCode());
    };
  }
}
