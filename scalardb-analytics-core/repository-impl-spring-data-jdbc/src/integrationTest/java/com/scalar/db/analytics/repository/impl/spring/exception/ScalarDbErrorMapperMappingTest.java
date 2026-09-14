/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.exception;

import static com.scalar.db.analytics.repository.impl.spring.constants.EntityTypes.CATALOG;
import static com.scalar.db.analytics.repository.impl.spring.constants.EntityTypes.NAMESPACE;
import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.repository.impl.spring.support.AbstractScalarDbIntegrationTest;
import java.sql.SQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.TransientDataAccessResourceException;

class ScalarDbErrorMapperMappingTest extends AbstractScalarDbIntegrationTest {

  @Autowired private SpringDataJdbcErrorMapper errorMapper;

  @Test
  @DisplayName("Duplicate catalog ID (primary key violation) → EntityAlreadyExists")
  void duplicateCatalogIdShouldMapToEntityAlreadyExists() {
    DuplicateKeyException duplicateKeyException =
        new DuplicateKeyException("duplicate key value violates unique constraint");

    AnalyticsException mapped = errorMapper.mapException(duplicateKeyException, CATALOG, "id");

    assertThat(mapped.getErrorCode()).isEqualTo(AnalyticsErrorCode.CATALOG_ALREADY_EXISTS);
  }

  @Test
  @DisplayName("Delete catalog with references → ConstraintViolation")
  void deleteCatalogWhenHasDataSourcesShouldMapToConstraintViolation() {
    DataIntegrityViolationException violation =
        new DataIntegrityViolationException(
            "update or delete on table \"catalogs\" violates foreign key constraint",
            new SQLException("violates foreign key constraint", "23503", 0));

    AnalyticsException mapped = errorMapper.mapException(violation, CATALOG, "ref-catalog");

    assertThat(mapped.getErrorCode()).isEqualTo(AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED);
  }

  @Test
  @DisplayName(
      "Create namespace with non-existent data source ID (foreign key violation) → ConstraintViolation")
  void createNamespaceWithNonExistentDataSourceShouldMapToConstraintViolation() {
    DataIntegrityViolationException violation =
        new DataIntegrityViolationException(
            "insert or update on table \"namespaces\" violates foreign key constraint",
            new SQLException("violates foreign key constraint", "23503", 0));

    AnalyticsException mapped = errorMapper.mapException(violation, NAMESPACE, null);

    assertThat(mapped.getErrorCode()).isEqualTo(AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED);
  }

  @Test
  @DisplayName("Database connection error → ConnectionError")
  void connectionErrorWhenDatabaseUnavailableShouldMapToConnectionError() {
    TransientDataAccessResourceException transientEx =
        new TransientDataAccessResourceException("Connection timeout") {
          private static final long serialVersionUID = 1L;
        };

    AnalyticsException mapped = errorMapper.mapException(transientEx, CATALOG, "test-catalog");
    assertThat(mapped.getErrorCode()).isEqualTo(AnalyticsErrorCode.ANALYTICS_DB_CONNECTION_FAILED);
  }

  @Test
  @DisplayName("Invalid SQL execution → DatabaseError")
  void unexpectedSqlErrorShouldMapToDatabaseError() {
    try {
      jdbcTemplate.execute("INVALID SQL STATEMENT");
    } catch (DataAccessException ex) {
      AnalyticsException mapped = errorMapper.mapException(ex, CATALOG, null);
      assertThat(mapped.getErrorCode()).isEqualTo(AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED);
      return;
    }

    throw new AssertionError("Expected DataAccessException to be thrown");
  }
}
