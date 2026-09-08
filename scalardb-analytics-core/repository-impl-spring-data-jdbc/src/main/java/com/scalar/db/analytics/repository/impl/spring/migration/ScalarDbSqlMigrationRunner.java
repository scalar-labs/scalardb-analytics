/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;

@Order(1)
public class ScalarDbSqlMigrationRunner implements ApplicationRunner {

  private static final Logger logger = LoggerFactory.getLogger(ScalarDbSqlMigrationRunner.class);

  private final ScalarDbSqlMigrator migrator;

  public ScalarDbSqlMigrationRunner(ScalarDbSqlMigrator migrator) {
    this.migrator = migrator;
  }

  @Override
  public void run(ApplicationArguments args) {
    logger.info("Executing ScalarDB SQL migrations");
    migrator.migrate();
  }
}
