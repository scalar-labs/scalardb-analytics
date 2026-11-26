/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.protobuf.Any;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.grpc.generated.catalog.v1.CatalogServiceGrpc;
import com.scalar.db.analytics.grpc.generated.catalog.v1.CreateCatalogRequest;
import com.scalar.db.analytics.grpc.generated.catalog.v1.CreateCatalogResponse;
import com.scalar.db.analytics.grpc.generated.error.v1.AnalyticsError;
import com.scalar.db.analytics.sdk.catalog.BlockingGrpcCatalogClient;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Checks that the retry policy built from {@link RetryConfig} retries a retryable server error as
 * the server sends it: gRPC {@code UNAVAILABLE} carrying an {@code AnalyticsError} detail, with no
 * response headers or messages. {@code ANALYTICS_DB_TRANSACTION_CONFLICT} is used as the example.
 */
class RetryOnRetryableServerErrorTest {

  private static final AnalyticsErrorCode CODE =
      AnalyticsErrorCode.ANALYTICS_DB_TRANSACTION_CONFLICT;

  private final String serverName = "retry-test-" + UUID.randomUUID();
  private final AtomicInteger calls = new AtomicInteger();
  private @Nullable Server server;
  private @Nullable ManagedChannel channel;

  /** Fails the first {@code failures} calls with {@link #CODE}, then succeeds. */
  private void startServer(int failures) throws Exception {
    CatalogServiceGrpc.CatalogServiceImplBase service =
        new CatalogServiceGrpc.CatalogServiceImplBase() {
          @Override
          public void createCatalog(
              CreateCatalogRequest request,
              StreamObserver<CreateCatalogResponse> responseObserver) {
            if (calls.incrementAndGet() <= failures) {
              // The same rich status ExceptionHandlingInterceptor builds for a retryable code.
              responseObserver.onError(
                  StatusProto.toStatusRuntimeException(
                      com.google.rpc.Status.newBuilder()
                          .setCode(Status.Code.UNAVAILABLE.value())
                          .setMessage(CODE.getCode() + ": Analytics database transaction conflict")
                          .addDetails(
                              Any.pack(
                                  AnalyticsError.newBuilder().setErrorCode(CODE.getCode()).build()))
                          .build()));
              return;
            }
            responseObserver.onNext(
                CreateCatalogResponse.newBuilder()
                    .setCatalog(
                        com.scalar.db.analytics.grpc.generated.catalog.v1.Catalog.newBuilder()
                            .setId(UUID.randomUUID().toString())
                            .setName(request.getCatalogName())
                            .build())
                    .build());
            responseObserver.onCompleted();
          }
        };
    server =
        InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(service)
            .build()
            .start();
  }

  private BlockingGrpcCatalogClient client(RetryConfig retryConfig) {
    InProcessChannelBuilder builder = InProcessChannelBuilder.forName(serverName).directExecutor();
    if (retryConfig.isEnabled()) {
      builder.enableRetry().defaultServiceConfig(retryConfig.toServiceConfig());
    } else {
      builder.disableRetry();
    }
    ManagedChannel built = builder.build();
    channel = built;
    return new BlockingGrpcCatalogClient(built);
  }

  private static RetryConfig fastRetry() {
    return RetryConfig.builder().initialBackoffMillis(1).maxBackoffMillis(10).build();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (channel != null) {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (server != null) {
      server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void retryableServerError_shouldBeRetriedUntilTheCallSucceeds() throws Exception {
    startServer(2);

    Catalog catalog = client(fastRetry()).createCatalog("c");

    assertThat(catalog.getName()).isEqualTo("c");
    assertThat(calls.get()).isEqualTo(3);
  }

  @Test
  void retryableServerError_shouldSurfaceItsCodeOnceAttemptsAreExhausted() throws Exception {
    startServer(Integer.MAX_VALUE);
    RetryConfig retryConfig = fastRetry();

    assertThatThrownBy(() -> client(retryConfig).createCatalog("c"))
        .isInstanceOfSatisfying(
            AnalyticsException.class, e -> assertThat(e.getErrorCode()).isEqualTo(CODE));
    assertThat(calls.get()).isEqualTo(retryConfig.getMaxAttempts());
  }

  @Test
  void retryableServerError_shouldNotBeRetriedWhenRetryIsDisabled() throws Exception {
    startServer(Integer.MAX_VALUE);

    assertThatThrownBy(() -> client(RetryConfig.disabled()).createCatalog("c"))
        .isInstanceOfSatisfying(
            AnalyticsException.class, e -> assertThat(e.getErrorCode()).isEqualTo(CODE));
    assertThat(calls.get()).isEqualTo(1);
  }
}
