/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
/**
 * Error code infrastructure for ScalarDB Analytics.
 *
 * <p>Provides a unified error model using {@link
 * com.scalar.db.analytics.api.error.AnalyticsErrorCode} and {@link
 * com.scalar.db.analytics.api.error.AnalyticsException} that all layers share. User-facing wording
 * (message, cause, action) is colocated with each error code via {@link
 * com.scalar.db.analytics.api.error.ErrorDescription} and is not part of the API contract.
 */
package com.scalar.db.analytics.api.error;
