/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.auth.AccessToken;
import com.scalar.db.analytics.grpc.generated.auth.v1.AuthServiceGrpc;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClientAuthInterceptorTest {

  @Mock private TokenManager tokenManager;
  @Mock private Channel channel;

  @SuppressWarnings("rawtypes")
  @Mock
  private ClientCall clientCall;

  private ClientAuthInterceptor interceptor;

  @BeforeEach
  void setUp() {
    interceptor = new ClientAuthInterceptor(tokenManager);
  }

  @SuppressWarnings("unchecked")
  @Test
  void interceptCall_ShouldAddAuthHeaders_WhenNotAuthService() {
    // Arrange
    AccessToken token = new AccessToken("test-token", Instant.now().plusSeconds(3600), "user-123");
    when(tokenManager.getToken()).thenReturn(token);

    MethodDescriptor<Object, Object> method = createMethod("catalog.v1.CatalogService/ListAll");
    when(channel.newCall(eq(method), any(CallOptions.class))).thenReturn(clientCall);

    // Act
    ClientCall<Object, Object> result =
        interceptor.interceptCall(method, CallOptions.DEFAULT, channel);
    Metadata headers = new Metadata();
    result.start(mock(ClientCall.Listener.class), headers);

    // Assert
    assertThat(headers.get(ClientAuthInterceptor.AUTHORIZATION_KEY)).isEqualTo("Bearer test-token");
    assertThat(headers.get(ClientAuthInterceptor.USER_ID_KEY)).isEqualTo("user-123");
  }

  @SuppressWarnings("unchecked")
  @Test
  void interceptCall_ShouldSkipAuthHeaders_WhenAuthService() {
    // Arrange
    MethodDescriptor<Object, Object> method =
        createMethod(AuthServiceGrpc.SERVICE_NAME + "/AuthenticateWithPassword");
    when(channel.newCall(eq(method), any(CallOptions.class))).thenReturn(clientCall);

    // Act
    ClientCall<Object, Object> result =
        interceptor.interceptCall(method, CallOptions.DEFAULT, channel);

    // Assert - the returned call should be the original, not wrapped
    assertThat(result).isSameAs(clientCall);
  }

  @SuppressWarnings("unchecked")
  @Test
  void interceptCall_ShouldPassHeadersToDelegate() {
    // Arrange
    AccessToken token = new AccessToken("my-token", Instant.now().plusSeconds(3600), "uid-456");
    when(tokenManager.getToken()).thenReturn(token);

    MethodDescriptor<Object, Object> method = createMethod("datasource.v1.DataSourceService/List");
    when(channel.newCall(eq(method), any(CallOptions.class))).thenReturn(clientCall);

    @SuppressWarnings("rawtypes")
    ClientCall.Listener listener = mock(ClientCall.Listener.class);

    // Act
    ClientCall<Object, Object> result =
        interceptor.interceptCall(method, CallOptions.DEFAULT, channel);
    Metadata headers = new Metadata();
    result.start(listener, headers);

    // Assert - verify delegate was called with the enriched headers
    ArgumentCaptor<Metadata> metadataCaptor = ArgumentCaptor.forClass(Metadata.class);
    verify(clientCall).start(eq(listener), metadataCaptor.capture());
    Metadata capturedHeaders = metadataCaptor.getValue();
    assertThat(capturedHeaders.get(ClientAuthInterceptor.AUTHORIZATION_KEY))
        .isEqualTo("Bearer my-token");
    assertThat(capturedHeaders.get(ClientAuthInterceptor.USER_ID_KEY)).isEqualTo("uid-456");
  }

  @SuppressWarnings("unchecked")
  private static MethodDescriptor<Object, Object> createMethod(String fullMethodName) {
    return MethodDescriptor.newBuilder()
        .setType(MethodDescriptor.MethodType.UNARY)
        .setFullMethodName(fullMethodName)
        .setRequestMarshaller(mock(MethodDescriptor.Marshaller.class))
        .setResponseMarshaller(mock(MethodDescriptor.Marshaller.class))
        .build();
  }
}
