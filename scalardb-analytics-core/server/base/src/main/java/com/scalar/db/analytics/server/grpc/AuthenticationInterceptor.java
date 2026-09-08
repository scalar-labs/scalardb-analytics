/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.grpc.generated.auth.v1.AuthServiceGrpc;
import com.scalar.db.analytics.usecase.auth.TokenValidationUseCase;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.health.v1.HealthGrpc;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A gRPC server interceptor that authenticates requests using Bearer tokens.
 *
 * <p>Extracts the token from the {@code Authorization: Bearer <token>} header, validates it via
 * {@link TokenValidationUseCase}, and propagates the authenticated user ID in {@link
 * io.grpc.Context}.
 */
public class AuthenticationInterceptor implements ServerInterceptor {

  /** Context key for the authenticated user ID. */
  public static final Context.Key<UUID> AUTHENTICATED_USER_ID =
      Context.key("authenticated-user-id");

  static final Metadata.Key<String> AUTHORIZATION_HEADER =
      Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

  static final Metadata.Key<String> USER_ID_HEADER =
      Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER);

  private static final String BEARER_PREFIX = "Bearer ";

  private static final Set<String> PUBLIC_SERVICES =
      Set.of(AuthServiceGrpc.SERVICE_NAME, HealthGrpc.SERVICE_NAME);

  private final TokenValidationUseCase tokenValidationUseCase;

  public AuthenticationInterceptor(TokenValidationUseCase tokenValidationUseCase) {
    this.tokenValidationUseCase = tokenValidationUseCase;
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

    String fullMethodName = call.getMethodDescriptor().getFullMethodName();

    if (isPublicService(fullMethodName)) {
      return next.startCall(call, headers);
    }

    UUID userId = extractUserId(headers);
    String token = extractBearerToken(headers);
    validateToken(userId, token);
    Context ctx = Context.current().withValue(AUTHENTICATED_USER_ID, userId);
    return Contexts.interceptCall(ctx, call, headers, next);
  }

  private boolean isPublicService(String fullMethodName) {
    String serviceName = extractServiceName(fullMethodName);
    return PUBLIC_SERVICES.contains(serviceName);
  }

  private String extractBearerToken(Metadata headers) {
    String authHeader = headers.get(AUTHORIZATION_HEADER);
    if (authHeader == null) {
      throw new AnalyticsException(
          AnalyticsErrorCode.TOKEN_INVALID, Map.of("reason", "missing_authorization_header"));
    }
    if (!authHeader.startsWith(BEARER_PREFIX)) {
      throw new AnalyticsException(
          AnalyticsErrorCode.TOKEN_INVALID,
          Map.of("reason", "invalid_authorization_header_format"));
    }
    String token = authHeader.substring(BEARER_PREFIX.length()).trim();
    if (token.isEmpty()) {
      throw new AnalyticsException(
          AnalyticsErrorCode.TOKEN_INVALID, Map.of("reason", "missing_bearer_token"));
    }
    return token;
  }

  private UUID extractUserId(Metadata headers) {
    String userIdHeader = headers.get(USER_ID_HEADER);
    if (userIdHeader == null) {
      throw new AnalyticsException(
          AnalyticsErrorCode.TOKEN_INVALID, Map.of("reason", "missing_user_id_header"));
    }
    try {
      return UUID.fromString(userIdHeader);
    } catch (IllegalArgumentException e) {
      throw new AnalyticsException(
          AnalyticsErrorCode.TOKEN_INVALID, Map.of("reason", "invalid_user_id_header_format"), e);
    }
  }

  private void validateToken(UUID userId, String token) {
    tokenValidationUseCase.validateToken(userId, token);
  }

  private static String extractServiceName(String fullMethodName) {
    int slashIndex = fullMethodName.lastIndexOf('/');
    if (slashIndex >= 0) {
      return fullMethodName.substring(0, slashIndex);
    }
    return fullMethodName;
  }
}
