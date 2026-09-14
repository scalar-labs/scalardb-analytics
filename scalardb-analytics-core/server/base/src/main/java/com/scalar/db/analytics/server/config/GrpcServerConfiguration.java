/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.config;

import com.scalar.db.analytics.server.config.auth.AuthServerProperties;
import com.scalar.db.analytics.server.grpc.CatalogServiceImpl;
import com.scalar.db.analytics.server.grpc.DataSourceServiceImpl;
import com.scalar.db.analytics.server.grpc.HealthService;
import com.scalar.db.analytics.server.grpc.NamespaceServiceImpl;
import com.scalar.db.analytics.server.grpc.TableServiceImpl;
import com.scalar.db.analytics.usecase.CatalogUseCase;
import com.scalar.db.analytics.usecase.DataSourceUseCase;
import com.scalar.db.analytics.usecase.NamespaceUseCase;
import com.scalar.db.analytics.usecase.TableUseCase;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
  CatalogServerProperties.class,
  ServerTlsProperties.class,
  ServerCommonProperties.class,
  AuthServerProperties.class
})
public class GrpcServerConfiguration {

  @Bean
  public CatalogServiceImpl catalogService(CatalogUseCase catalogUseCase) {
    return new CatalogServiceImpl(catalogUseCase);
  }

  @Bean
  public DataSourceServiceImpl dataSourceService(DataSourceUseCase dataSourceUseCase) {
    return new DataSourceServiceImpl(dataSourceUseCase);
  }

  @Bean
  public NamespaceServiceImpl namespaceService(NamespaceUseCase namespaceUseCase) {
    return new NamespaceServiceImpl(namespaceUseCase);
  }

  @Bean
  public TableServiceImpl tableService(TableUseCase tableUseCase) {
    return new TableServiceImpl(tableUseCase);
  }

  @Bean
  public HealthService healthService() {
    return new HealthService();
  }
}
