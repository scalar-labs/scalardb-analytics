/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.auth.PasswordCredential;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.grpc.common.HealthCheckClient;
import com.scalar.db.analytics.sdk.auth.TokenManager;
import io.grpc.ManagedChannel;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlockingGrpcAnalyticsClientTest {

  @Mock private ManagedChannel channel;
  private ScalarDbAnalyticsClient client;

  @BeforeEach
  void setUp() {
    client = new BlockingGrpcAnalyticsClient(channel);
  }

  @Test
  void catalog_ShouldReturnCatalogClient() {
    // Act & Assert
    assertThat(client.catalog()).isNotNull();
  }

  @Test
  void dataSource_ShouldReturnDataSourceClient() {
    // Act & Assert
    assertThat(client.dataSource()).isNotNull();
  }

  @Test
  void namespace_ShouldReturnNamespaceClient() {
    // Act & Assert
    assertThat(client.namespace()).isNotNull();
  }

  @Test
  void table_ShouldReturnTableClient() {
    // Act & Assert
    assertThat(client.table()).isNotNull();
  }

  @Test
  void internalUserDirectory_ShouldReturnInternalUserDirectoryClient() {
    // Act & Assert
    assertThat(client.internalUserDirectory()).isNotNull();
  }

  @Test
  void healthCheck_ShouldReturnNonEmptyOptional() {
    // Act
    Optional<HealthCheckClient> result = client.healthCheck();

    // Assert
    assertThat(result).isPresent();
  }

  @Test
  void close_ShouldShutdownChannel() throws Exception {
    // Arrange
    when(channel.isShutdown()).thenReturn(false);
    when(channel.awaitTermination(5, TimeUnit.SECONDS)).thenReturn(true);

    // Act
    client.close();

    // Assert
    verify(channel).shutdown();
    verify(channel).awaitTermination(5, TimeUnit.SECONDS);
  }

  @Test
  void close_ShouldShutdownNow_WhenTerminationTimeout() throws Exception {
    // Arrange
    when(channel.isShutdown()).thenReturn(false);
    when(channel.awaitTermination(5, TimeUnit.SECONDS)).thenReturn(false);

    // Act
    client.close();

    // Assert
    verify(channel).shutdown();
    verify(channel).shutdownNow();
  }

  @Test
  void close_ShouldShutdownNow_WhenInterrupted() throws Exception {
    // Arrange
    when(channel.isShutdown()).thenReturn(false);
    when(channel.awaitTermination(5, TimeUnit.SECONDS)).thenThrow(new InterruptedException());

    // Act
    client.close();

    // Assert
    verify(channel).shutdown();
    verify(channel).shutdownNow();
  }

  @Test
  void close_ShouldCloseTokenManager_WhenAuthEnabled() throws Exception {
    // Arrange
    TokenManager tokenManager = mock(TokenManager.class);
    ScalarDbAnalyticsClient authClient =
        new BlockingGrpcAnalyticsClient(channel, channel, tokenManager);
    when(channel.isShutdown()).thenReturn(false);
    when(channel.awaitTermination(5, TimeUnit.SECONDS)).thenReturn(true);

    // Act
    authClient.close();

    // Assert
    verify(tokenManager).close();
    verify(channel).shutdown();
  }

  @Test
  void close_ShouldNotShutdown_WhenAlreadyShutdown() throws Exception {
    // Arrange
    when(channel.isShutdown()).thenReturn(true);

    // Act
    client.close();

    // Assert
    verify(channel).isShutdown();
  }

  @Test
  void builder_ShouldThrowException_WhenHostIsNull() {
    // Act & Assert
    assertThatThrownBy(() -> ScalarDbAnalyticsClient.builder().port(8080).build())
        .isInstanceOf(AnalyticsException.class)
        .extracting(e -> ((AnalyticsException) e).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.INVALID_ARGUMENT);
  }

  @Test
  void builder_ShouldThrowException_WhenHostIsEmpty() {
    // Act & Assert
    assertThatThrownBy(() -> ScalarDbAnalyticsClient.builder().host("").port(8080).build())
        .isInstanceOf(AnalyticsException.class)
        .extracting(e -> ((AnalyticsException) e).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.INVALID_ARGUMENT);
  }

  @Test
  void build_ShouldShutdownChannel_WhenAuthenticationFails() {
    // Arrange
    ManagedChannel mockChannel = mock(ManagedChannel.class);
    BlockingGrpcAnalyticsClient.BuilderImpl builder =
        spy(new BlockingGrpcAnalyticsClient.BuilderImpl());
    doReturn(mockChannel).when(builder).createChannel();

    builder.host("localhost");
    builder.port(8080);
    builder.passwordCredential(new PasswordCredential("user", "pass"));

    // Act & Assert
    assertThatThrownBy(builder::build).isInstanceOf(AnalyticsException.class);
    verify(mockChannel).shutdownNow();
  }

  @Test
  void builder_ShouldThrowException_WhenPortIsInvalid() {
    // Act & Assert
    assertThatThrownBy(() -> ScalarDbAnalyticsClient.builder().host("localhost").port(0).build())
        .isInstanceOf(AnalyticsException.class)
        .extracting(e -> ((AnalyticsException) e).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.INVALID_ARGUMENT);

    assertThatThrownBy(
            () -> ScalarDbAnalyticsClient.builder().host("localhost").port(65536).build())
        .isInstanceOf(AnalyticsException.class)
        .extracting(e -> ((AnalyticsException) e).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.INVALID_ARGUMENT);

    assertThatThrownBy(() -> ScalarDbAnalyticsClient.builder().host("localhost").port(-1).build())
        .isInstanceOf(AnalyticsException.class)
        .extracting(e -> ((AnalyticsException) e).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.INVALID_ARGUMENT);
  }
}
