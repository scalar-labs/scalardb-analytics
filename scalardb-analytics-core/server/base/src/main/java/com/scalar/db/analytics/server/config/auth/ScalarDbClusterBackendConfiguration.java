/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.config.auth;

import com.scalar.db.analytics.grpc.generated.scalardb.cluster.rpc.v1.auth.AuthLoginGrpc;
import com.scalar.db.analytics.server.auth.ScalarDbClusterPasswordBackend;
import com.scalar.db.analytics.usecase.auth.PasswordAuthenticationBackend;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import java.io.File;
import javax.net.ssl.SSLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
    prefix = "scalar.db.analytics.server.auth",
    name = "enabled",
    havingValue = "true")
@ConditionalOnProperty(
    prefix = "scalar.db.analytics.server.auth.password",
    name = "backend",
    havingValue = "scalardb-cluster")
@EnableConfigurationProperties(ScalarDbClusterProperties.class)
public class ScalarDbClusterBackendConfiguration {

  private static final Logger logger =
      LoggerFactory.getLogger(ScalarDbClusterBackendConfiguration.class);

  @Bean(destroyMethod = "shutdown")
  public ManagedChannel scalarDbClusterChannel(ScalarDbClusterProperties properties)
      throws SSLException {
    String target = properties.getHost() + ":" + properties.getPort();

    if (properties.getTls().isEnabled()) {
      SslContextBuilder sslContextBuilder = GrpcSslContexts.forClient();

      String caRootCertPath = properties.getTls().getCaRootCertPath();
      if (caRootCertPath != null) {
        sslContextBuilder.trustManager(new File(caRootCertPath));
      }

      SslContext sslContext = sslContextBuilder.build();
      NettyChannelBuilder channelBuilder =
          NettyChannelBuilder.forTarget(target).sslContext(sslContext);

      String overrideAuthority = properties.getTls().getOverrideAuthority();
      if (overrideAuthority != null) {
        channelBuilder.overrideAuthority(overrideAuthority);
      }

      return channelBuilder.build();
    }

    logger.warn(
        "TLS is disabled for the ScalarDB Cluster backend channel (target: {}). "
            + "User credentials will be transmitted in plaintext. "
            + "Enable TLS in production environments.",
        target);
    return ManagedChannelBuilder.forTarget(target).usePlaintext().build();
  }

  @Bean
  public PasswordAuthenticationBackend passwordAuthenticationBackend(
      ManagedChannel scalarDbClusterChannel, ScalarDbClusterProperties properties) {
    AuthLoginGrpc.AuthLoginBlockingStub stub =
        AuthLoginGrpc.newBlockingStub(scalarDbClusterChannel);
    return new ScalarDbClusterPasswordBackend(stub, properties.getDeadlineMillis());
  }
}
