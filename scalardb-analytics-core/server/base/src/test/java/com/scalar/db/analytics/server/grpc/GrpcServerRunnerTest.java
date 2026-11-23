/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.scalar.db.analytics.server.config.ServerCommonProperties;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class GrpcServerRunnerTest {

  private static GrpcServerRunner runner(ManagedGrpcServer... servers) {
    return new GrpcServerRunner(List.of(servers), mock(ServerCommonProperties.class), List.of());
  }

  @Test
  void start_LaterServerFails_ReleasesPortOfEarlierServer() throws Exception {
    NetworkServer earlier = new NetworkServer();
    ManagedGrpcServer failing = mock(ManagedGrpcServer.class);
    IOException failure = new IOException("bind failed");
    doThrow(failure).when(failing).start();

    assertThatThrownBy(() -> runner(earlier, failing).start()).isSameAs(failure);

    assertThat(earlier.server.isTerminated()).isTrue();
    try (ServerSocket socket = new ServerSocket(earlier.port)) {
      assertThat(socket.isBound()).isTrue();
    }
  }

  @Test
  void start_LaterServerFails_StopsStartedServersInReverseOrderOnly() throws Exception {
    ManagedGrpcServer first = mock(ManagedGrpcServer.class);
    ManagedGrpcServer second = mock(ManagedGrpcServer.class);
    ManagedGrpcServer failing = mock(ManagedGrpcServer.class);
    ManagedGrpcServer notStarted = mock(ManagedGrpcServer.class);
    doThrow(new IllegalStateException("failed")).when(failing).start();

    assertThatThrownBy(() -> runner(first, second, failing, notStarted).start())
        .isInstanceOf(IllegalStateException.class);

    InOrder inOrder = inOrder(first, second);
    inOrder.verify(second).stop();
    inOrder.verify(first).stop();
    verify(failing, never()).stop();
    verify(notStarted, never()).start();
    verify(notStarted, never()).stop();
  }

  @Test
  void start_StopFailsDuringCleanup_StopsRemainingServersAndKeepsStartupFailure() throws Exception {
    ManagedGrpcServer first = mock(ManagedGrpcServer.class);
    ManagedGrpcServer second = mock(ManagedGrpcServer.class);
    ManagedGrpcServer failing = mock(ManagedGrpcServer.class);
    IOException failure = new IOException("bind failed");
    IllegalStateException stopFailure = new IllegalStateException("stop failed");
    doThrow(failure).when(failing).start();
    doThrow(stopFailure).when(second).stop();

    assertThatThrownBy(() -> runner(first, second, failing).start())
        .isSameAs(failure)
        .hasSuppressedException(stopFailure);

    verify(first).stop();
  }

  @Test
  void start_StopInterruptedDuringCleanup_StopsRemainingServersBeforeRestoringInterrupt()
      throws Exception {
    ManagedGrpcServer first = mock(ManagedGrpcServer.class);
    ManagedGrpcServer second = mock(ManagedGrpcServer.class);
    ManagedGrpcServer failing = mock(ManagedGrpcServer.class);
    IOException failure = new IOException("bind failed");
    InterruptedException interruption = new InterruptedException("interrupted");
    AtomicBoolean firstStoppedWhileInterrupted = new AtomicBoolean(true);
    doThrow(failure).when(failing).start();
    doThrow(interruption).when(second).stop();
    doAnswer(
            invocation -> {
              firstStoppedWhileInterrupted.set(Thread.currentThread().isInterrupted());
              return null;
            })
        .when(first)
        .stop();

    try {
      assertThatThrownBy(() -> runner(first, second, failing).start())
          .isSameAs(failure)
          .hasSuppressedException(interruption);

      assertThat(firstStoppedWhileInterrupted).isFalse();
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void stop_StopInterrupted_StopsRemainingServersBeforeRestoringInterrupt() throws Exception {
    ManagedGrpcServer first = mock(ManagedGrpcServer.class);
    ManagedGrpcServer second = mock(ManagedGrpcServer.class);
    AtomicBoolean secondStoppedWhileInterrupted = new AtomicBoolean(true);
    doThrow(new InterruptedException("interrupted")).when(first).stop();
    doAnswer(
            invocation -> {
              secondStoppedWhileInterrupted.set(Thread.currentThread().isInterrupted());
              return null;
            })
        .when(second)
        .stop();

    try {
      runner(first, second).stop();

      assertThat(secondStoppedWhileInterrupted).isFalse();
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted();
    }
  }

  private static final class NetworkServer implements ManagedGrpcServer {
    private final ExecutorService executor = CatalogGrpcServer.newCallExecutor();
    private final Server server = ServerBuilder.forPort(0).executor(executor).build();
    private int port;

    @Override
    public void start() throws IOException {
      server.start();
      port = server.getPort();
    }

    @Override
    public void stop() throws InterruptedException {
      CatalogGrpcServer.shutdownAndAwaitTermination(server, executor, 10, TimeUnit.SECONDS);
    }

    @Override
    public void blockUntilShutdown() {}

    @Override
    @Nullable
    public HealthService getHealthService() {
      return null;
    }

    @Override
    public String getServerName() {
      return "Network server";
    }
  }
}
