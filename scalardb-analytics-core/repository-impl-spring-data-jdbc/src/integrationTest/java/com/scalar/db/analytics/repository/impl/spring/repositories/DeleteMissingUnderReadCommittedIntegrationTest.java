/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repositories;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
    properties =
        "scalar.db.analytics.server.db.parameters"
            + "[scalar.db.consensus_commit.isolation_level]=READ_COMMITTED")
class DeleteMissingUnderReadCommittedIntegrationTest extends AbstractDeleteMissingIntegrationTest {}
