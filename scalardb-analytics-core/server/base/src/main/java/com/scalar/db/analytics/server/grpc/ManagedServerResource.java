/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

/**
 * Marker for a bean that must stay usable until every {@link ManagedGrpcServer} has stopped.
 *
 * <p>{@link GrpcServerRunner} injects all implementations without calling them. The injection
 * exists only so that Spring destroys the runner, and therefore the gRPC servers, before it
 * destroys these resources.
 *
 * <p>The list is empty when nothing declares the requirement, which leaves the destruction order
 * unconstrained.
 */
public interface ManagedServerResource {}
