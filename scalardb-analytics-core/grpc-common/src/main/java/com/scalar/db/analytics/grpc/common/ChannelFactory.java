/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.common;

import com.scalar.db.analytics.grpc.common.exception.GrpcClientException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import java.io.File;
import javax.net.ssl.SSLException;
import org.jspecify.annotations.Nullable;

/** Factory for creating gRPC ManagedChannel instances with plaintext or TLS configurations. */
public final class ChannelFactory {

  private ChannelFactory() {
    // Utility class
  }

  /**
   * Creates a plaintext (insecure) gRPC channel.
   *
   * @param host the server host
   * @param port the server port
   * @return a ManagedChannel configured for plaintext communication
   */
  public static ManagedChannel createPlaintext(String host, int port) {
    return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
  }

  /**
   * Creates a TLS-enabled (secure) gRPC channel.
   *
   * @param host the server host
   * @param port the server port
   * @param tlsConfig TLS configuration (may contain CA cert path and override authority)
   * @return a ManagedChannel configured for TLS communication
   * @throws GrpcClientException if SSL context creation fails or certificate file is not found
   */
  public static ManagedChannel createTls(String host, int port, @Nullable TlsConfig tlsConfig) {
    return createTlsBuilder(host, port, tlsConfig).build();
  }

  /**
   * Creates a TLS-enabled (secure) gRPC channel builder for further customization.
   *
   * @param host the server host
   * @param port the server port
   * @param tlsConfig TLS configuration (may contain CA cert path and override authority)
   * @return a NettyChannelBuilder configured for TLS communication
   * @throws GrpcClientException if SSL context creation fails or certificate file is not found
   */
  public static NettyChannelBuilder createTlsBuilder(
      String host, int port, @Nullable TlsConfig tlsConfig) {
    try {
      SslContextBuilder sslContextBuilder = GrpcSslContexts.forClient();

      // If a custom trust certificate is provided, use it
      if (tlsConfig != null && tlsConfig.getCaRootCertPath() != null) {
        File trustCertFile = new File(tlsConfig.getCaRootCertPath());
        if (!trustCertFile.exists()) {
          throw new GrpcClientException(
              "Trust certificate file not found: " + trustCertFile.getAbsolutePath());
        }
        sslContextBuilder = sslContextBuilder.trustManager(trustCertFile);
      }

      // Configure strong cipher suites and protocols
      SslContext sslContext =
          sslContextBuilder
              .protocols("TLSv1.3", "TLSv1.2")
              .ciphers(null) // Use default strong ciphers only
              .build();

      NettyChannelBuilder channelBuilder =
          NettyChannelBuilder.forAddress(host, port).sslContext(sslContext);

      // Set override authority if specified
      if (tlsConfig != null && tlsConfig.getOverrideAuthority() != null) {
        channelBuilder.overrideAuthority(tlsConfig.getOverrideAuthority());
      }

      return channelBuilder;
    } catch (SSLException e) {
      throw new GrpcClientException("Failed to create secure channel", e);
    }
  }
}
