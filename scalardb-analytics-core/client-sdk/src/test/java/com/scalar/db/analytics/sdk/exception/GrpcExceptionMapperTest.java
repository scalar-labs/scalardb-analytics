/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.grpc.generated.error.v1.AnalyticsError;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import org.junit.jupiter.api.Test;

class GrpcExceptionMapperTest {

  private static StatusRuntimeException withDetail(
      Status.Code statusCode, String errorCode, java.util.Map<String, String> metadata) {
    AnalyticsError.Builder error = AnalyticsError.newBuilder().setErrorCode(errorCode);
    metadata.forEach(error::putMetadata);
    com.google.rpc.Status rpcStatus =
        com.google.rpc.Status.newBuilder()
            .setCode(statusCode.value())
            .addDetails(Any.pack(error.build()))
            .build();
    return StatusProto.toStatusRuntimeException(rpcStatus);
  }

  @Test
  void toAnalyticsException_ShouldReconstructFromDetail_IgnoringGrpcStatus() {
    // The gRPC status (PERMISSION_DENIED) and the domain code (ACCESS_DENIED) both describe the
    // same
    // failure; the mapper must take the domain code from the detail, not infer from the status.
    StatusRuntimeException sre =
        withDetail(
            Status.Code.PERMISSION_DENIED,
            AnalyticsErrorCode.ACCESS_DENIED.getCode(),
            java.util.Collections.singletonMap("user_id", "alice"));

    AnalyticsException result = GrpcExceptionMapper.toAnalyticsException(sre);

    assertThat(result.getErrorCode()).isEqualTo(AnalyticsErrorCode.ACCESS_DENIED);
    assertThat(result.getMetadata()).containsEntry("user_id", "alice");
    assertThat(result.getCause()).isSameAs(sre);
  }

  @Test
  void toAnalyticsException_ShouldReturnUnrecognizedServerError_WhenDetailCodeUnknown() {
    // The detail is present (the server was reached) but its code is unknown to this SDK. It must
    // not be misreported as a transport failure via status mapping; instead it surfaces as
    // UNRECOGNIZED_SERVER_ERROR with the original code preserved for diagnostics.
    StatusRuntimeException sre =
        withDetail(
            Status.Code.UNAVAILABLE,
            "DB-ANALYTICS-99999",
            java.util.Collections.singletonMap("user_id", "alice"));

    AnalyticsException result = GrpcExceptionMapper.toAnalyticsException(sre);

    assertThat(result.getErrorCode()).isEqualTo(AnalyticsErrorCode.UNRECOGNIZED_SERVER_ERROR);
    assertThat(result.getMetadata()).containsEntry("server_error_code", "DB-ANALYTICS-99999");
    assertThat(result.getMetadata()).containsEntry("user_id", "alice");
    assertThat(result.getCause()).isSameAs(sre);
  }

  @Test
  void toAnalyticsException_ShouldMapMalformedDetail_ToClientInternalError() {
    // The detail is typed as an AnalyticsError (so detail.is(AnalyticsError.class) is true) but its
    // bytes are truncated/corrupt, so unpacking throws InvalidProtocolBufferException. This is the
    // one reconstruct-from-detail branch not otherwise exercised; the SDK boundary must still yield
    // a domain exception (CLIENT_INTERNAL_ERROR), never leak the raw parse failure.
    Any malformedDetail =
        Any.newBuilder()
            .setTypeUrl(Any.pack(AnalyticsError.getDefaultInstance()).getTypeUrl())
            .setValue(ByteString.copyFrom(new byte[] {0x08})) // truncated varint for field 1
            .build();
    com.google.rpc.Status rpcStatus =
        com.google.rpc.Status.newBuilder()
            .setCode(Status.Code.INTERNAL.value())
            .addDetails(malformedDetail)
            .build();
    StatusRuntimeException sre = StatusProto.toStatusRuntimeException(rpcStatus);

    AnalyticsException result = GrpcExceptionMapper.toAnalyticsException(sre);

    assertThat(result.getErrorCode()).isEqualTo(AnalyticsErrorCode.CLIENT_INTERNAL_ERROR);
    assertThat(result.getCause()).isSameAs(sre);
  }

  @Test
  void toAnalyticsException_ShouldAttachCallerContext_OnDetaillessTransportFailure() {
    // A detail-less transport failure carries no server metadata, so the caller's operation context
    // is the only thing that names the failed call. It must surface in the exception metadata.
    StatusRuntimeException sre = new StatusRuntimeException(Status.UNAVAILABLE);

    AnalyticsException result =
        GrpcExceptionMapper.toAnalyticsException(
            sre, "findDataSourceByName", java.util.Collections.singletonMap("catalogName", "prod"));

    assertThat(result.getErrorCode()).isEqualTo(AnalyticsErrorCode.SERVER_UNREACHABLE);
    assertThat(result.getMetadata())
        .containsEntry(GrpcExceptionMapper.OPERATION_METADATA_KEY, "findDataSourceByName")
        .containsEntry("catalogName", "prod");
  }

  @Test
  void toAnalyticsException_ShouldNotAttachCallerContext_WhenServerReturnedDetail() {
    // When the server returned structured metadata, that metadata is authoritative; the caller
    // context is not merged in (the operation name is not added to a server-derived error).
    StatusRuntimeException sre =
        withDetail(
            Status.Code.PERMISSION_DENIED,
            AnalyticsErrorCode.ACCESS_DENIED.getCode(),
            java.util.Collections.singletonMap("user_id", "alice"));

    AnalyticsException result =
        GrpcExceptionMapper.toAnalyticsException(
            sre, "grantPermission", java.util.Collections.singletonMap("roleName", "reader"));

    assertThat(result.getErrorCode()).isEqualTo(AnalyticsErrorCode.ACCESS_DENIED);
    assertThat(result.getMetadata()).containsEntry("user_id", "alice");
    assertThat(result.getMetadata()).doesNotContainKey(GrpcExceptionMapper.OPERATION_METADATA_KEY);
    assertThat(result.getMetadata()).doesNotContainKey("roleName");
  }

  @Test
  void toAnalyticsException_ShouldMapUnavailable_WhenNoDetail() {
    StatusRuntimeException sre = new StatusRuntimeException(Status.UNAVAILABLE);

    AnalyticsException result = GrpcExceptionMapper.toAnalyticsException(sre);

    assertThat(result.getErrorCode()).isEqualTo(AnalyticsErrorCode.SERVER_UNREACHABLE);
  }

  @Test
  void toAnalyticsException_ShouldMapDeadlineExceeded_WhenNoDetail() {
    StatusRuntimeException sre = new StatusRuntimeException(Status.DEADLINE_EXCEEDED);

    AnalyticsException result = GrpcExceptionMapper.toAnalyticsException(sre);

    assertThat(result.getErrorCode()).isEqualTo(AnalyticsErrorCode.REQUEST_TIMEOUT);
  }

  @Test
  void toAnalyticsException_ShouldMapOtherStatusToClientInternalError_WhenNoDetail() {
    StatusRuntimeException sre = new StatusRuntimeException(Status.INTERNAL);

    AnalyticsException result = GrpcExceptionMapper.toAnalyticsException(sre);

    assertThat(result.getErrorCode()).isEqualTo(AnalyticsErrorCode.CLIENT_INTERNAL_ERROR);
  }

  @Test
  void toAnalyticsException_ShouldMapNonGrpcException_ToClientInternalError() {
    AnalyticsException result =
        GrpcExceptionMapper.toAnalyticsException(new RuntimeException("boom"));

    assertThat(result.getErrorCode()).isEqualTo(AnalyticsErrorCode.CLIENT_INTERNAL_ERROR);
  }

  @Test
  void toAnalyticsException_ShouldPassThrough_WhenAlreadyAnalyticsException() {
    AnalyticsException original = new AnalyticsException(AnalyticsErrorCode.CATALOG_NOT_EMPTY);

    AnalyticsException result = GrpcExceptionMapper.toAnalyticsException(original);

    assertThat(result).isSameAs(original);
  }
}
