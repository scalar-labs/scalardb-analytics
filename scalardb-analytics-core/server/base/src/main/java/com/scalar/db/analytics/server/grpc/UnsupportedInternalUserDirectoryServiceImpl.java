/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.grpc.generated.auth.v1.CreateInternalBackendUserRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.CreateInternalBackendUserResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteInternalBackendUserRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteInternalBackendUserResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.InternalUserDirectoryServiceGrpc.InternalUserDirectoryServiceImplBase;
import com.scalar.db.analytics.service.authz.AuthorizationService;
import io.grpc.stub.StreamObserver;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The internal user directory service of a server configured with a password backend other than the
 * internal one.
 *
 * <p>Every call fails with {@link AnalyticsErrorCode#INTERNAL_PASSWORD_BACKEND_REQUIRED}. Without
 * this service, the methods would not be registered and the calls would fail with a bare gRPC
 * {@code UNIMPLEMENTED} that does not name the configuration the operation requires.
 */
public class UnsupportedInternalUserDirectoryServiceImpl
    extends InternalUserDirectoryServiceImplBase {

  private final PasswordBackendType configuredBackend;
  private final AuthorizationService authorizationService;

  public UnsupportedInternalUserDirectoryServiceImpl(
      PasswordBackendType configuredBackend, AuthorizationService authorizationService) {
    this.configuredBackend = configuredBackend;
    this.authorizationService = authorizationService;
  }

  @Override
  public void createInternalBackendUser(
      CreateInternalBackendUserRequest request,
      StreamObserver<CreateInternalBackendUserResponse> responseObserver) {
    throw internalPasswordBackendRequired();
  }

  @Override
  public void deleteInternalBackendUser(
      DeleteInternalBackendUserRequest request,
      StreamObserver<DeleteInternalBackendUserResponse> responseObserver) {
    throw internalPasswordBackendRequired();
  }

  /**
   * Returns the error for a call. Like the internal backend's use case, a caller other than a
   * superadmin gets ACCESS_DENIED, so the response does not reveal the server's configuration to
   * callers who could not use the operation anyway.
   */
  private AnalyticsException internalPasswordBackendRequired() {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    if (userId == null || !authorizationService.authorizeSuperAdmin(userId)) {
      return new AnalyticsException(
          AnalyticsErrorCode.ACCESS_DENIED,
          userId == null ? Map.of() : Map.of("user_id", userId.toString()));
    }
    return new AnalyticsException(
        AnalyticsErrorCode.INTERNAL_PASSWORD_BACKEND_REQUIRED,
        Map.of("password_backend", configuredBackend.name().toLowerCase(Locale.ROOT)));
  }
}
