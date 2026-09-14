/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    exclude = {org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration.class})
public class SpringDataJdbcIntegrationTestConfiguration {}
