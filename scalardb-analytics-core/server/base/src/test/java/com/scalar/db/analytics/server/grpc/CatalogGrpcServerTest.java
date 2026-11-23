/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerServiceDefinition;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class CatalogGrpcServerTest {
  private static final MethodDescriptor.Marshaller<byte[]> MARSHALLER =
      new MethodDescriptor.Marshaller<>() {
        @Override
        public InputStream stream(byte[] value) {
          return new ByteArrayInputStream(value);
        }

        @Override
        public byte[] parse(InputStream stream) {
          try {
            return stream.readAllBytes();
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        }
      };
  private static final MethodDescriptor<byte[], byte[]> HANG_METHOD =
      MethodDescriptor.<byte[], byte[]>newBuilder()
          .setType(MethodDescriptor.MethodType.UNARY)
          .setFullMethodName("test.Hang/Hang")
          .setRequestMarshaller(MARSHALLER)
          .setResponseMarshaller(MARSHALLER)
          .build();

  private static final long HANDLER_WORK_AFTER_RELEASE_MILLIS = 200;

  private final CountDownLatch handlerEntered = new CountDownLatch(1);
  private final CountDownLatch releaseHandler = new CountDownLatch(1);
  private final CountDownLatch handlerInterrupted = new CountDownLatch(1);
  private final AtomicBoolean handlerCompleted = new AtomicBoolean();
  private ExecutorService executor;
  private Server server;
  private ManagedChannel channel;

  @BeforeEach
  void setUp() throws Exception {
    String name = InProcessServerBuilder.generateName();
    executor = CatalogGrpcServer.newCallExecutor();
    server =
        InProcessServerBuilder.forName(name)
            .executor(executor)
            .addService(
                ServerServiceDefinition.builder("test.Hang")
                    .addMethod(HANG_METHOD, ServerCalls.asyncUnaryCall(this::hang))
                    .build())
            .build()
            .start();
    channel = InProcessChannelBuilder.forName(name).build();
  }

  @AfterEach
  void tearDown() {
    releaseHandler.countDown();
    channel.shutdownNow();
    server.shutdownNow();
    executor.shutdownNow();
  }

  /**
   * Blocks until released, ignoring the call's cancellation, then keeps working for a while as a
   * handler that does not check for cancellation would.
   */
  private void hang(byte[] request, StreamObserver<byte[]> responseObserver) {
    handlerEntered.countDown();
    try {
      releaseHandler.await();
      Thread.sleep(HANDLER_WORK_AFTER_RELEASE_MILLIS);
      handlerCompleted.set(true);
    } catch (InterruptedException e) {
      handlerInterrupted.countDown();
    }
  }

  private void startHangingCall() throws InterruptedException {
    ClientCalls.asyncUnaryCall(
        channel.newCall(HANG_METHOD, CallOptions.DEFAULT),
        new byte[0],
        new StreamObserver<byte[]>() {
          @Override
          public void onNext(byte[] value) {}

          @Override
          public void onError(Throwable t) {}

          @Override
          public void onCompleted() {}
        });
    assertThat(handlerEntered.await(10, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  void shutdownAndAwaitTermination_GracefulTimeoutExpires_WaitsForCancelledHandlerToFinish()
      throws Exception {
    startHangingCall();
    // Release the handler only once the forced shutdown has terminated the server, so that the
    // handler is still running when the server reports termination.
    Thread releaser =
        new Thread(
            () -> {
              try {
                server.awaitTermination();
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
              releaseHandler.countDown();
            });
    releaser.start();

    CatalogGrpcServer.shutdownAndAwaitTermination(server, executor, 1, TimeUnit.SECONDS);

    assertThat(server.isTerminated()).isTrue();
    assertThat(handlerCompleted).isTrue();
    releaser.join();
  }

  @Test
  void shutdownAndAwaitTermination_HandlerDoesNotFinish_InterruptsHandler() throws Exception {
    startHangingCall();

    CatalogGrpcServer.shutdownAndAwaitTermination(server, executor, 200, TimeUnit.MILLISECONDS);

    assertThat(server.isTerminated()).isTrue();
    assertThat(handlerInterrupted.await(10, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  void shutdownAndAwaitTermination_ServerNeverTerminates_ReturnsAfterForcedShutdown()
      throws Exception {
    Server neverTerminating = mock(Server.class);
    when(neverTerminating.shutdown()).thenReturn(neverTerminating);
    when(neverTerminating.awaitTermination(1, TimeUnit.MILLISECONDS)).thenReturn(false);

    CatalogGrpcServer.shutdownAndAwaitTermination(
        neverTerminating, executor, 1, TimeUnit.MILLISECONDS);

    InOrder inOrder = inOrder(neverTerminating);
    inOrder.verify(neverTerminating).awaitTermination(1, TimeUnit.MILLISECONDS);
    inOrder.verify(neverTerminating).shutdownNow();
    inOrder.verify(neverTerminating).awaitTermination(1, TimeUnit.MILLISECONDS);
  }

  @Test
  void shutdownAndAwaitTermination_NoRunningCall_Terminates() throws Exception {
    CatalogGrpcServer.shutdownAndAwaitTermination(server, executor, 10, TimeUnit.SECONDS);

    assertThat(server.isTerminated()).isTrue();
    assertThat(executor.isTerminated()).isTrue();
  }

  @Test
  void shutdownAndAwaitTermination_Interrupted_ForcesShutdownAndRethrows() throws Exception {
    startHangingCall();

    Thread.currentThread().interrupt();
    assertThatThrownBy(
            () ->
                CatalogGrpcServer.shutdownAndAwaitTermination(
                    server, executor, 10, TimeUnit.SECONDS))
        .isInstanceOf(InterruptedException.class);

    assertThat(server.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    assertThat(handlerInterrupted.await(10, TimeUnit.SECONDS)).isTrue();
  }
}
