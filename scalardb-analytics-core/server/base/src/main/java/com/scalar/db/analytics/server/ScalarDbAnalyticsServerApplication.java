/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server;

import com.scalar.db.analytics.repository.impl.spring.autoconfigure.SpringDataJdbcRepositoryAutoConfiguration;
import com.scalar.db.analytics.server.grpc.GrpcServerRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@ConfigurationPropertiesScan
@Import({SpringDataJdbcRepositoryAutoConfiguration.class})
public class ScalarDbAnalyticsServerApplication {

  public static void run(String[] args) {
    var app = new SpringApplication(ScalarDbAnalyticsServerApplication.class);
    app.setBannerMode(Banner.Mode.OFF);
    var context = app.run(args);

    // Block until shutdown
    try {
      var grpcServerRunner = context.getBean(GrpcServerRunner.class);
      grpcServerRunner.blockUntilShutdown();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Server interrupted", e);
    }
  }
}
