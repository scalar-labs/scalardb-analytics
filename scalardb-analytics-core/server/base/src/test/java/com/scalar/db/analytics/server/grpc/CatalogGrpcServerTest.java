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
import java.util.concurrent.TimeUnit;
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

  private final CountDownLatch handlerEntered = new CountDownLatch(1);
  private final CountDownLatch releaseHandler = new CountDownLatch(1);
  private Server server;
  private ManagedChannel channel;

  @BeforeEach
  void setUp() throws Exception {
    String name = InProcessServerBuilder.generateName();
    server =
        InProcessServerBuilder.forName(name)
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
  }

  private void hang(byte[] request, StreamObserver<byte[]> responseObserver) {
    handlerEntered.countDown();
    try {
      releaseHandler.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
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
  void shutdownAndAwaitTermination_GracefulTimeoutExpires_ForcesShutdownAndAwaitsTermination()
      throws Exception {
    startHangingCall();

    CatalogGrpcServer.shutdownAndAwaitTermination(server, 200, TimeUnit.MILLISECONDS);

    assertThat(server.isTerminated()).isTrue();
  }

  @Test
  void shutdownAndAwaitTermination_ServerNeverTerminates_ReturnsAfterForcedShutdown()
      throws Exception {
    Server neverTerminating = mock(Server.class);
    when(neverTerminating.shutdown()).thenReturn(neverTerminating);
    when(neverTerminating.awaitTermination(1, TimeUnit.MILLISECONDS)).thenReturn(false);

    CatalogGrpcServer.shutdownAndAwaitTermination(neverTerminating, 1, TimeUnit.MILLISECONDS);

    InOrder inOrder = inOrder(neverTerminating);
    inOrder.verify(neverTerminating).awaitTermination(1, TimeUnit.MILLISECONDS);
    inOrder.verify(neverTerminating).shutdownNow();
    inOrder.verify(neverTerminating).awaitTermination(1, TimeUnit.MILLISECONDS);
  }

  @Test
  void shutdownAndAwaitTermination_NoRunningCall_Terminates() throws Exception {
    CatalogGrpcServer.shutdownAndAwaitTermination(server, 10, TimeUnit.SECONDS);

    assertThat(server.isTerminated()).isTrue();
  }

  @Test
  void shutdownAndAwaitTermination_Interrupted_ForcesShutdownAndRethrows() throws Exception {
    startHangingCall();

    Thread.currentThread().interrupt();
    assertThatThrownBy(
            () -> CatalogGrpcServer.shutdownAndAwaitTermination(server, 10, TimeUnit.SECONDS))
        .isInstanceOf(InterruptedException.class);

    assertThat(server.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
  }
}
