/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.auth;

import com.scalar.db.analytics.api.auth.AccessToken;
import com.scalar.db.analytics.grpc.generated.auth.v1.AuthServiceGrpc;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

/**
 * A gRPC client interceptor that attaches authentication headers to outgoing requests.
 *
 * <p>Adds {@code Authorization: Bearer <token>} and {@code x-user-id: <uuid>} headers to all
 * requests except those directed at the {@link AuthServiceGrpc AuthService}.
 */
public class ClientAuthInterceptor implements ClientInterceptor {

  static final Metadata.Key<String> AUTHORIZATION_KEY =
      Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

  static final Metadata.Key<String> USER_ID_KEY =
      Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER);

  private static final String BEARER_PREFIX = "Bearer ";

  private final TokenManager tokenManager;

  public ClientAuthInterceptor(TokenManager tokenManager) {
    this.tokenManager = tokenManager;
  }

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

    if (AuthServiceGrpc.SERVICE_NAME.equals(method.getServiceName())) {
      return next.newCall(method, callOptions);
    }

    return new SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        AccessToken token = tokenManager.getToken();
        headers.put(AUTHORIZATION_KEY, BEARER_PREFIX + token.getToken());
        headers.put(USER_ID_KEY, token.getUserId());
        super.start(responseListener, headers);
      }
    };
  }
}
