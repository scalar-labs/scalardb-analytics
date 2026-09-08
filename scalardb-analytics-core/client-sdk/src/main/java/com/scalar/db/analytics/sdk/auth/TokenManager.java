/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.auth;

import com.google.common.annotations.VisibleForTesting;
import com.scalar.db.analytics.api.auth.AccessToken;
import com.scalar.db.analytics.api.auth.PasswordCredential;
import com.scalar.db.analytics.api.error.AnalyticsException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.Nullable;

/**
 * Manages the lifecycle of an authentication token, including initial acquisition and automatic
 * renewal.
 *
 * <p>The token is automatically refreshed when 75% of its validity period has elapsed. Refresh
 * failures are logged but do not throw exceptions; the existing token continues to be used until it
 * expires.
 *
 * <p>When a {@code tokenCacheDir} is provided, tokens are persisted to disk via {@link
 * FileTokenCache} so that short-lived processes (e.g., CLI invocations) can reuse valid tokens
 * without re-authenticating.
 *
 * <p>This class is thread-safe. The current token is stored in a volatile field to ensure
 * visibility across threads.
 */
public class TokenManager implements AutoCloseable {
  private static final Logger logger = Logger.getLogger(TokenManager.class.getName());

  @VisibleForTesting static final double REFRESH_THRESHOLD = 0.75;
  @VisibleForTesting static final long MIN_REMAINING_SECONDS = 30;

  private final AuthClient authClient;
  private final PasswordCredential credential;
  private final ScheduledExecutorService scheduler;
  private final TokenCache tokenCache;

  // Assigned in initialize(), which must be called before the token is read.
  @SuppressWarnings("NullAway.Init")
  private volatile AccessToken currentToken;

  public TokenManager(AuthClient authClient, PasswordCredential credential) {
    this(authClient, credential, (Path) null);
  }

  public TokenManager(
      AuthClient authClient, PasswordCredential credential, @Nullable Path tokenCacheDir) {
    this(
        authClient,
        credential,
        tokenCacheDir,
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "token-refresh");
              t.setDaemon(true);
              return t;
            }));
  }

  @VisibleForTesting
  TokenManager(
      AuthClient authClient,
      PasswordCredential credential,
      @Nullable Path tokenCacheDir,
      ScheduledExecutorService scheduler) {
    this.authClient = authClient;
    this.credential = credential;
    this.scheduler = scheduler;
    this.tokenCache =
        tokenCacheDir != null ? new FileTokenCache(tokenCacheDir) : new NoOpTokenCache();
  }

  @VisibleForTesting
  TokenManager(
      AuthClient authClient,
      PasswordCredential credential,
      ScheduledExecutorService scheduler,
      TokenCache tokenCache) {
    this.authClient = authClient;
    this.credential = credential;
    this.scheduler = scheduler;
    this.tokenCache = tokenCache;
  }

  /**
   * Performs the initial authentication and schedules the first token refresh.
   *
   * <p>If a valid cached token is available in the {@link TokenCache}, it is used instead of making
   * an authentication RPC. A cached token is considered valid if it has more than 30 seconds
   * remaining before expiry.
   *
   * @throws com.scalar.db.analytics.api.error.AnalyticsException if authentication fails
   */
  public void initialize() {
    AccessToken cached = tokenCache.load(credential.getUsername());
    if (cached != null && !isExpiredOrAlmostExpired(cached)) {
      currentToken = cached;
    } else {
      currentToken = authClient.authenticate(credential);
      tokenCache.save(credential.getUsername(), currentToken);
    }
    scheduleRefresh();
  }

  /** Returns the current access token. */
  public AccessToken getToken() {
    return currentToken;
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void scheduleRefresh() {
    // Guard against RejectedExecutionException when close() has been called.
    // This can happen if a running refresh() is interrupted by shutdownNow()
    // and then attempts to reschedule.
    if (scheduler.isShutdown()) {
      return;
    }
    long delaySeconds = calculateRefreshDelay();
    if (delaySeconds >= 0) {
      scheduler.schedule(this::refresh, delaySeconds, TimeUnit.SECONDS);
    }
  }

  @VisibleForTesting
  long calculateRefreshDelay() {
    Instant now = Instant.now();
    long remainingSeconds = currentToken.getExpiresAt().getEpochSecond() - now.getEpochSecond();
    if (remainingSeconds <= 0) {
      return 0;
    }
    return (long) (remainingSeconds * REFRESH_THRESHOLD);
  }

  private void refresh() {
    try {
      currentToken = authClient.authenticate(credential);
      tokenCache.save(credential.getUsername(), currentToken);
    } catch (AnalyticsException e) {
      if (e.getErrorCode().isUserError()) {
        // Non-retryable: credentials are invalid (e.g., password changed by admin).
        // Stop the refresh loop since retrying with the same credentials will not succeed.
        logger.log(Level.SEVERE, "Token refresh stopped: authentication rejected by server", e);
        return;
      }
      logger.log(Level.WARNING, "Token refresh failed", e);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Token refresh failed", e);
    }
    scheduleRefresh();
  }

  private static boolean isExpiredOrAlmostExpired(AccessToken token) {
    long remainingSeconds = token.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond();
    return remainingSeconds <= MIN_REMAINING_SECONDS;
  }

  @Override
  public void close() {
    scheduler.shutdownNow();
  }
}
