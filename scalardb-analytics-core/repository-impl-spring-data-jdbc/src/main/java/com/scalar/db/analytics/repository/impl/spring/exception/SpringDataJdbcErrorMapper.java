/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.exception;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

/**
 * Maps Spring Data JDBC exceptions to {@link AnalyticsException} with appropriate error codes.
 *
 * <p>Note: EmptyResultDataAccessException and OptimisticLockingFailureException are handled as
 * ANALYTICS_DB_OPERATION_FAILED since the current implementation: - Returns Optional.empty() for
 * entity not found scenarios - Does not use optimistic locking (@Version)
 */
@Component
public class SpringDataJdbcErrorMapper {

  private final DatabaseSpecificErrorAnalyzer errorAnalyzer;

  public SpringDataJdbcErrorMapper(DatabaseSpecificErrorAnalyzer errorAnalyzer) {
    this.errorAnalyzer = errorAnalyzer;
  }

  /**
   * Maps a DataAccessException to an AnalyticsException with appropriate error code.
   *
   * @param e the DataAccessException to map
   * @param entityType the type of entity involved in the operation
   * @param identifier the identifier of the entity (can be null)
   * @return an AnalyticsException with structured error information
   */
  public AnalyticsException mapException(
      DataAccessException e, String entityType, @Nullable String identifier) {

    // First, try database-specific error categorization
    Optional<AnalyticsException> specificError =
        errorAnalyzer.categorizeError(e, entityType, identifier);
    if (specificError.isPresent()) {
      return specificError.get();
    }

    // Handle specific Spring DAO exceptions
    return mapSpringException(e, entityType, identifier);
  }

  /**
   * Maps a general Exception to an AnalyticsException. This is used for non-Spring DAO exceptions.
   *
   * @param e the Exception to map
   * @param entityType the type of entity involved in the operation
   * @param identifier the identifier of the entity (can be null)
   * @return an AnalyticsException with structured error information
   */
  public AnalyticsException mapException(
      Exception e, String entityType, @Nullable String identifier) {
    // Check if the exception contains a DataAccessException as its cause
    Throwable current = e;
    while (current != null) {
      if (current instanceof DataAccessException dataAccessException) {
        return mapException(dataAccessException, entityType, identifier);
      }
      current = current.getCause();
    }

    // For non-DataAccessException, create a generic database error
    return new AnalyticsException(AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED, e);
  }

  /**
   * Returns the entity-specific "already exists" error code for the given entity type.
   *
   * @param entityType the entity type string (e.g., "Catalog", "DataSource")
   * @return the corresponding error code
   */
  static AnalyticsErrorCode entityAlreadyExistsCode(String entityType) {
    switch (entityType) {
      case "Catalog":
        return AnalyticsErrorCode.CATALOG_ALREADY_EXISTS;
      case "DataSource":
        return AnalyticsErrorCode.DATA_SOURCE_ALREADY_EXISTS;
      case "Namespace":
        return AnalyticsErrorCode.NAMESPACE_ALREADY_EXISTS;
      case "Table":
        return AnalyticsErrorCode.TABLE_ALREADY_EXISTS;
      case "Role":
        return AnalyticsErrorCode.ROLE_ALREADY_EXISTS;
      case "RoleAssignment":
        return AnalyticsErrorCode.ROLE_ALREADY_ASSIGNED;
      case "AccessControlEntry":
        return AnalyticsErrorCode.PERMISSION_ALREADY_GRANTED;
      case "PasswordIdentity":
      case "InternalCredential":
        return AnalyticsErrorCode.USER_ALREADY_EXISTS;
      default:
        return AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED;
    }
  }

  private AnalyticsException mapSpringException(
      DataAccessException e, String entityType, @Nullable String identifier) {
    return switch (e) {
      case DuplicateKeyException ignored ->
          new AnalyticsException(
              entityAlreadyExistsCode(entityType),
              Map.of("entity_name", identifier != null ? identifier : "unknown"),
              e);

      case DataIntegrityViolationException ignored ->
          new AnalyticsException(AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED, e);

      case TransientDataAccessException ignored ->
          new AnalyticsException(AnalyticsErrorCode.ANALYTICS_DB_CONNECTION_FAILED, e);

      default -> {
        // Try to extract SQL error information
        Throwable rootCause = e.getRootCause();
        if (rootCause instanceof SQLException sqlEx) {
          yield new AnalyticsException(
              AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED,
              Map.of("sql_state", sqlEx.getSQLState() != null ? sqlEx.getSQLState() : "unknown"),
              e);
        }

        yield new AnalyticsException(AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED, e);
      }
    };
  }
}
