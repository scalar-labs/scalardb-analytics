/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.auth.AccessToken;
import com.scalar.db.analytics.api.auth.PasswordCredential;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenManagerTest {

  @Mock private AuthClient authClient;
  @Mock private ScheduledExecutorService scheduler;

  private PasswordCredential credential;
  private TokenManager tokenManager;

  @BeforeEach
  void setUp() {
    credential = new PasswordCredential("user", "pass");
    tokenManager = new TokenManager(authClient, credential, scheduler, new NoOpTokenCache());
  }

  @Test
  void initialize_ShouldAuthenticateAndStoreToken() {
    // Arrange
    AccessToken token = new AccessToken("token-1", Instant.now().plusSeconds(3600), "user-id-1");
    when(authClient.authenticate(credential)).thenReturn(token);

    // Act
    tokenManager.initialize();

    // Assert
    assertThat(tokenManager.getToken()).isSameAs(token);
  }

  @Test
  void initialize_ShouldScheduleRefresh() {
    // Arrange
    AccessToken token = new AccessToken("token-1", Instant.now().plusSeconds(3600), "user-id-1");
    when(authClient.authenticate(credential)).thenReturn(token);

    // Act
    tokenManager.initialize();

    // Assert
    verify(scheduler).schedule(any(Runnable.class), anyLong(), eq(TimeUnit.SECONDS));
  }

  @Test
  void initialize_ShouldThrowException_WhenAuthenticationFails() {
    // Arrange
    when(authClient.authenticate(credential))
        .thenThrow(new AnalyticsException(AnalyticsErrorCode.AUTHENTICATION_FAILED));

    // Act & Assert
    assertThatThrownBy(() -> tokenManager.initialize())
        .isInstanceOf(AnalyticsException.class)
        .extracting(e -> ((AnalyticsException) e).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.AUTHENTICATION_FAILED);
  }

  @Test
  void getToken_ShouldReturnCurrentToken() {
    // Arrange
    AccessToken token = new AccessToken("token-1", Instant.now().plusSeconds(3600), "user-id-1");
    when(authClient.authenticate(credential)).thenReturn(token);
    tokenManager.initialize();

    // Act & Assert
    assertThat(tokenManager.getToken().getToken()).isEqualTo("token-1");
    assertThat(tokenManager.getToken().getUserId()).isEqualTo("user-id-1");
  }

  @Test
  void calculateRefreshDelay_ShouldReturn75PercentOfRemainingTime() {
    // Arrange
    AccessToken token = new AccessToken("token-1", Instant.now().plusSeconds(1000), "user-id-1");
    when(authClient.authenticate(credential)).thenReturn(token);
    tokenManager.initialize();

    // Act
    long delay = tokenManager.calculateRefreshDelay();

    // Assert - should be approximately 750 seconds (75% of 1000)
    assertThat(delay).isBetween(740L, 760L);
  }

  @Test
  void calculateRefreshDelay_ShouldReturnZero_WhenTokenExpired() {
    // Arrange
    AccessToken token = new AccessToken("token-1", Instant.now().minusSeconds(100), "user-id-1");
    when(authClient.authenticate(credential)).thenReturn(token);
    tokenManager.initialize();

    // Act
    long delay = tokenManager.calculateRefreshDelay();

    // Assert
    assertThat(delay).isEqualTo(0);
  }

  @Test
  void close_ShouldShutdownScheduler() {
    // Act
    tokenManager.close();

    // Assert
    verify(scheduler).shutdownNow();
  }

  @Test
  void refresh_ShouldStopRefreshLoop_WhenUserErrorOccurs() {
    // Arrange
    AccessToken token = new AccessToken("token-1", Instant.now().plusSeconds(3600), "user-id-1");
    ScheduledFuture<?> future = mock(ScheduledFuture.class);
    doReturn(future).when(scheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    when(authClient.authenticate(credential)).thenReturn(token);
    tokenManager.initialize();

    // Capture the refresh Runnable scheduled during initialize()
    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(scheduler).schedule(captor.capture(), anyLong(), eq(TimeUnit.SECONDS));

    // Make subsequent authenticate() throw a user error (credentials rejected)
    when(authClient.authenticate(credential))
        .thenThrow(new AnalyticsException(AnalyticsErrorCode.AUTHENTICATION_FAILED));

    // Act - execute the captured refresh task
    captor.getValue().run();

    // Assert - scheduler.schedule() should NOT have been called again (only the 1 from initialize)
    verify(scheduler, times(1)).schedule(any(Runnable.class), anyLong(), eq(TimeUnit.SECONDS));
  }

  @Test
  void refresh_ShouldContinueRefreshLoop_WhenTransientErrorOccurs() {
    // Arrange
    AccessToken token = new AccessToken("token-1", Instant.now().plusSeconds(3600), "user-id-1");
    ScheduledFuture<?> future = mock(ScheduledFuture.class);
    doReturn(future).when(scheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    when(authClient.authenticate(credential)).thenReturn(token);
    tokenManager.initialize();

    // Capture the refresh Runnable
    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(scheduler).schedule(captor.capture(), anyLong(), eq(TimeUnit.SECONDS));

    // Make subsequent authenticate() throw a non-user (transient) error
    when(authClient.authenticate(credential))
        .thenThrow(new AnalyticsException(AnalyticsErrorCode.SERVER_UNREACHABLE));

    // Act
    captor.getValue().run();

    // Assert - scheduler.schedule() should have been called again (2 total: initialize + retry)
    verify(scheduler, times(2)).schedule(any(Runnable.class), anyLong(), eq(TimeUnit.SECONDS));
  }

  @Nested
  class WithTokenCache {

    @Mock private TokenCache tokenCache;

    @Test
    void initialize_ShouldUseCachedToken_WhenStoreHasValidToken() {
      // Arrange
      AccessToken cached =
          new AccessToken("cached-token", Instant.now().plusSeconds(3600), "cached-user-id");
      when(tokenCache.load("user")).thenReturn(cached);
      TokenManager manager = new TokenManager(authClient, credential, scheduler, tokenCache);

      // Act
      manager.initialize();

      // Assert
      assertThat(manager.getToken()).isSameAs(cached);
      verify(authClient, never()).authenticate(any());
    }

    @Test
    void initialize_ShouldAuthenticate_WhenCachedTokenIsExpired() {
      // Arrange
      AccessToken expired =
          new AccessToken("expired-token", Instant.now().minusSeconds(100), "user-id");
      when(tokenCache.load("user")).thenReturn(expired);
      AccessToken fresh =
          new AccessToken("fresh-token", Instant.now().plusSeconds(3600), "user-id");
      when(authClient.authenticate(credential)).thenReturn(fresh);
      TokenManager manager = new TokenManager(authClient, credential, scheduler, tokenCache);

      // Act
      manager.initialize();

      // Assert
      assertThat(manager.getToken()).isSameAs(fresh);
      verify(authClient).authenticate(credential);
      verify(tokenCache).save("user", fresh);
    }

    @Test
    void initialize_ShouldAuthenticate_WhenCachedTokenAlmostExpired() {
      // Arrange - token expires in 20 seconds (below MIN_REMAINING_SECONDS of 30)
      AccessToken almostExpired =
          new AccessToken("almost-expired", Instant.now().plusSeconds(20), "user-id");
      when(tokenCache.load("user")).thenReturn(almostExpired);
      AccessToken fresh =
          new AccessToken("fresh-token", Instant.now().plusSeconds(3600), "user-id");
      when(authClient.authenticate(credential)).thenReturn(fresh);
      TokenManager manager = new TokenManager(authClient, credential, scheduler, tokenCache);

      // Act
      manager.initialize();

      // Assert
      assertThat(manager.getToken()).isSameAs(fresh);
      verify(authClient).authenticate(credential);
      verify(tokenCache).save("user", fresh);
    }

    @Test
    void initialize_ShouldAuthenticate_WhenStoreReturnsNull() {
      // Arrange
      when(tokenCache.load("user")).thenReturn(null);
      AccessToken fresh =
          new AccessToken("fresh-token", Instant.now().plusSeconds(3600), "user-id");
      when(authClient.authenticate(credential)).thenReturn(fresh);
      TokenManager manager = new TokenManager(authClient, credential, scheduler, tokenCache);

      // Act
      manager.initialize();

      // Assert
      assertThat(manager.getToken()).isSameAs(fresh);
      verify(authClient).authenticate(credential);
      verify(tokenCache).save("user", fresh);
    }

    @Test
    void initialize_ShouldSaveTokenToStore_AfterAuthentication() {
      // Arrange
      when(tokenCache.load("user")).thenReturn(null);
      AccessToken token = new AccessToken("new-token", Instant.now().plusSeconds(3600), "user-id");
      when(authClient.authenticate(credential)).thenReturn(token);
      TokenManager manager = new TokenManager(authClient, credential, scheduler, tokenCache);

      // Act
      manager.initialize();

      // Assert
      verify(tokenCache).save("user", token);
    }

    @Test
    void initialize_ShouldNotSaveToStore_WhenUsingCachedToken() {
      // Arrange
      AccessToken cached =
          new AccessToken("cached-token", Instant.now().plusSeconds(3600), "cached-user-id");
      when(tokenCache.load("user")).thenReturn(cached);
      TokenManager manager = new TokenManager(authClient, credential, scheduler, tokenCache);

      // Act
      manager.initialize();

      // Assert
      verify(tokenCache, never()).save(any(), any());
    }
  }
}
