/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.config;

import com.scalar.db.analytics.repository.impl.spring.transaction.ConflictTranslatingTransactionManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;

/**
 * Makes the {@code @Transactional} repository methods report a commit-time transaction conflict as
 * a retryable error. See {@link ConflictTranslatingTransactionManager}.
 *
 * <p>The delegate is the primary transaction manager, the one these methods used before. It is
 * resolved lazily: Spring calls {@link #annotationDrivenTransactionManager()} while it builds the
 * transaction infrastructure, before regular beans are created. This class is kept separate from
 * {@link SpringDataJdbcConfiguration} for the same reason, so that only it is created that early.
 */
@Configuration(proxyBeanMethods = false)
public class AnnotationDrivenTransactionManagerConfiguration
    implements TransactionManagementConfigurer {

  private final ObjectProvider<PlatformTransactionManager> transactionManager;

  public AnnotationDrivenTransactionManagerConfiguration(
      ObjectProvider<PlatformTransactionManager> transactionManager) {
    this.transactionManager = transactionManager;
  }

  @Override
  public TransactionManager annotationDrivenTransactionManager() {
    return new ConflictTranslatingTransactionManager(transactionManager::getObject);
  }
}
