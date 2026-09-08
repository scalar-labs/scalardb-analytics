/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.autoconfigure;

import com.scalar.db.analytics.repository.impl.spring.boot.SpringDataJdbcRepositoryConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration(after = DataSourceTransactionManagerAutoConfiguration.class)
@ConditionalOnClass(org.springframework.data.jdbc.core.JdbcAggregateOperations.class)
@EnableConfigurationProperties(ScalarDbSqlProperties.class)
@Import(SpringDataJdbcRepositoryConfiguration.class)
public class SpringDataJdbcRepositoryAutoConfiguration {}
