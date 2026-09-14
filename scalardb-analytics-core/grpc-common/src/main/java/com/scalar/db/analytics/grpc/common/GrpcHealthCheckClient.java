/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.common;

import com.scalar.db.analytics.grpc.common.exception.HealthCheckException;
import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.health.v1.HealthGrpc;
import java.util.concurrent.TimeUnit;

/** gRPC-based implementation of HealthCheckClient. */
public class GrpcHealthCheckClient implements HealthCheckClient {
  private final HealthGrpc.HealthBlockingStub healthStub;

  public GrpcHealthCheckClient(Channel channel) {
    this.healthStub = HealthGrpc.newBlockingStub(channel);
  }

  @Override
  public boolean check() throws HealthCheckException {
    return check("");
  }

  @Override
  public boolean check(String serviceName) throws HealthCheckException {
    try {
      HealthCheckRequest request = HealthCheckRequest.newBuilder().setService(serviceName).build();
      HealthCheckResponse response =
          healthStub.withDeadlineAfter(5, TimeUnit.SECONDS).check(request);
      return response.getStatus() == ServingStatus.SERVING;
    } catch (StatusRuntimeException e) {
      throw new HealthCheckException(
          "Health check failed for service '" + serviceName + "': " + e.getStatus(), e);
    } catch (Exception e) {
      throw new HealthCheckException(
          "Unexpected error during health check for service '" + serviceName + "'", e);
    }
  }
}
