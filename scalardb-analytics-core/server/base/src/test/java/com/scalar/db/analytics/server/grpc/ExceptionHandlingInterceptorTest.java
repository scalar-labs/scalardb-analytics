/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.grpc.generated.catalog.v1.CatalogServiceGrpc;
import com.scalar.db.analytics.grpc.generated.catalog.v1.CreateCatalogRequest;
import com.scalar.db.analytics.grpc.generated.catalog.v1.CreateCatalogResponse;
import com.scalar.db.analytics.grpc.generated.error.v1.AnalyticsError;
import io.grpc.Channel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

class ExceptionHandlingInterceptorTest {

  private static final String SERVER_NAME = "test-server-" + UUID.randomUUID();

  private Server server;
  private CatalogServiceGrpc.CatalogServiceBlockingStub stub;
  private AutoCloseable mocks;

  @BeforeEach
  void setUp() throws IOException {
    mocks = MockitoAnnotations.openMocks(this);

    // Create a test service that will throw exceptions
    TestCatalogServiceImpl testService = new TestCatalogServiceImpl();

    // Create server with interceptor
    server =
        InProcessServerBuilder.forName(SERVER_NAME)
            .intercept(new ExceptionHandlingInterceptor())
            .addService(testService)
            .build()
            .start();

    // Create client
    Channel channel = InProcessChannelBuilder.forName(SERVER_NAME).build();
    stub = CatalogServiceGrpc.newBlockingStub(channel);
  }

  @AfterEach
  void tearDown() throws Exception {
    if (server != null) {
      server.shutdownNow();
    }
    if (mocks != null) {
      mocks.close();
    }
  }

  @Test
  void testDeletionBlockedException_mapsToFailedPrecondition() {
    // Act & Assert
    assertThatThrownBy(
            () ->
                stub.createCatalog(
                    CreateCatalogRequest.newBuilder().setCatalogName("deletion-blocked").build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.FAILED_PRECONDITION.getCode());
              assertThat(sre.getStatus().getDescription()).contains("Data source is not empty");
              assertThat(errorCodeOf(sre))
                  .isEqualTo(AnalyticsErrorCode.DATA_SOURCE_NOT_EMPTY.getCode());
            });
  }

  @Test
  void testConnectionError_mapsToUnavailable() {
    // Act & Assert
    assertThatThrownBy(
            () ->
                stub.createCatalog(
                    CreateCatalogRequest.newBuilder().setCatalogName("connection-error").build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.UNAVAILABLE.getCode());
              assertThat(sre.getStatus().getDescription())
                  .contains("Analytics database connection failed");
              assertThat(errorCodeOf(sre))
                  .isEqualTo(AnalyticsErrorCode.ANALYTICS_DB_CONNECTION_FAILED.getCode());
            });
  }

  @Test
  void testEntityAlreadyExists_mapsToAlreadyExists() {
    // Act & Assert
    assertThatThrownBy(
            () ->
                stub.createCatalog(
                    CreateCatalogRequest.newBuilder().setCatalogName("already-exists").build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.ALREADY_EXISTS.getCode());
              assertThat(sre.getStatus().getDescription()).contains("Catalog already exists");
              assertThat(errorCodeOf(sre))
                  .isEqualTo(AnalyticsErrorCode.CATALOG_ALREADY_EXISTS.getCode());
            });
  }

  @Test
  void testConstraintViolation_mapsToFailedPrecondition() {
    // Act & Assert
    assertThatThrownBy(
            () ->
                stub.createCatalog(
                    CreateCatalogRequest.newBuilder()
                        .setCatalogName("constraint-violation")
                        .build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.FAILED_PRECONDITION.getCode());
              assertThat(sre.getStatus().getDescription()).contains("Catalog is not empty");
              assertThat(errorCodeOf(sre))
                  .isEqualTo(AnalyticsErrorCode.CATALOG_NOT_EMPTY.getCode());
            });
  }

  @Test
  void testDatabaseError_mapsToInternal() {
    // Act & Assert
    assertThatThrownBy(
            () ->
                stub.createCatalog(
                    CreateCatalogRequest.newBuilder().setCatalogName("database-error").build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.INTERNAL.getCode());
              assertThat(sre.getStatus().getDescription())
                  .contains("Analytics database operation failed");
              assertThat(errorCodeOf(sre))
                  .isEqualTo(AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED.getCode());
            });
  }

  @Test
  void testDatabaseError_withoutDetail_mapsToInternal() {
    // Act & Assert
    assertThatThrownBy(
            () ->
                stub.createCatalog(
                    CreateCatalogRequest.newBuilder().setCatalogName("no-error-details").build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.INTERNAL.getCode());
              assertThat(sre.getStatus().getDescription())
                  .contains("Analytics database operation failed");
              assertThat(errorCodeOf(sre))
                  .isEqualTo(AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED.getCode());
            });
  }

  @Test
  void testSchemaResolutionFailure_mapsToInvalidArgument() {
    // Act & Assert
    assertThatThrownBy(
            () ->
                stub.createCatalog(
                    CreateCatalogRequest.newBuilder().setCatalogName("schema-error").build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.INVALID_ARGUMENT.getCode());
              assertThat(sre.getStatus().getDescription()).contains("Data source is unreachable");
              assertThat(errorCodeOf(sre))
                  .isEqualTo(AnalyticsErrorCode.DATA_SOURCE_UNREACHABLE.getCode());
            });
  }

  @Test
  void testIllegalArgumentException_mapsToInternal() {
    // Invalid caller input is thrown as AnalyticsException; a bare IllegalArgumentException is a
    // server-side defect.
    // Act & Assert
    assertThatThrownBy(
            () ->
                stub.createCatalog(
                    CreateCatalogRequest.newBuilder().setCatalogName("illegal-argument").build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.INTERNAL.getCode());
              assertThat(errorCodeOf(sre)).isEqualTo(AnalyticsErrorCode.INTERNAL_ERROR.getCode());
            });
  }

  @Test
  void testNotFound_mapsToNotFoundWithMetadata() {
    assertThatThrownBy(
            () ->
                stub.createCatalog(
                    CreateCatalogRequest.newBuilder().setCatalogName("role-not-found").build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
              assertThat(sre.getStatus().getDescription())
                  .isEqualTo("DB-ANALYTICS-10504: Role not found [role_name=analyst]");
              AnalyticsError error = analyticsErrorOf(sre);
              assertThat(error.getErrorCode())
                  .isEqualTo(AnalyticsErrorCode.ROLE_NOT_FOUND.getCode());
              assertThat(error.getMetadataMap()).isEqualTo(Map.of("role_name", "analyst"));
            });
  }

  @Test
  void testInvalidArgumentWithMetadata_mapsToInvalidArgumentWithMetadata() {
    assertThatThrownBy(
            () ->
                stub.createCatalog(
                    CreateCatalogRequest.newBuilder().setCatalogName("blank-role-name").build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.INVALID_ARGUMENT.getCode());
              AnalyticsError error = analyticsErrorOf(sre);
              assertThat(error.getErrorCode())
                  .isEqualTo(AnalyticsErrorCode.INVALID_ARGUMENT.getCode());
              assertThat(error.getMetadataMap())
                  .isEqualTo(Map.of("field", "role_name", "reason", "blank"));
            });
  }

  @Test
  void testGenericException_mapsToInternal() {
    // Act & Assert
    assertThatThrownBy(
            () ->
                stub.createCatalog(
                    CreateCatalogRequest.newBuilder().setCatalogName("generic-error").build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.INTERNAL.getCode());
              assertThat(sre.getStatus().getDescription()).contains("Unexpected error");
              assertThat(errorCodeOf(sre)).isEqualTo(AnalyticsErrorCode.INTERNAL_ERROR.getCode());
            });
  }

  @Test
  void testAccessDeniedException_mapsToPermissionDenied() {
    // Act & Assert
    assertThatThrownBy(
            () ->
                stub.createCatalog(
                    CreateCatalogRequest.newBuilder().setCatalogName("access-denied").build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.PERMISSION_DENIED.getCode());
              assertThat(sre.getStatus().getDescription()).contains("Access denied");
              assertThat(errorCodeOf(sre)).isEqualTo(AnalyticsErrorCode.ACCESS_DENIED.getCode());
            });
  }

  @Test
  void testScalarDbPrivilegeCheckFailure_withUnavailableCause_mapsToInternal() {
    assertThatThrownBy(
            () ->
                stub.createCatalog(
                    CreateCatalogRequest.newBuilder()
                        .setCatalogName("privilege-check-unavailable")
                        .build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.INTERNAL.getCode());
              assertThat(sre.getStatus().getDescription()).contains("privilege check failed");
              assertThat(errorCodeOf(sre))
                  .isEqualTo(AnalyticsErrorCode.SCALARDB_PRIVILEGE_CHECK_FAILURE.getCode());
            });
  }

  @Test
  void testScalarDbPrivilegeCheckFailure_withOtherCause_mapsToInternal() {
    assertThatThrownBy(
            () ->
                stub.createCatalog(
                    CreateCatalogRequest.newBuilder()
                        .setCatalogName("privilege-check-other")
                        .build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.INTERNAL.getCode());
              assertThat(sre.getStatus().getDescription()).contains("privilege check failed");
              assertThat(errorCodeOf(sre))
                  .isEqualTo(AnalyticsErrorCode.SCALARDB_PRIVILEGE_CHECK_FAILURE.getCode());
            });
  }

  @Test
  void testInvalidCredentials_mapsToUnauthenticated() {
    assertThatThrownBy(
            () ->
                stub.createCatalog(
                    CreateCatalogRequest.newBuilder()
                        .setCatalogName("invalid-credentials")
                        .build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.UNAUTHENTICATED.getCode());
              assertThat(sre.getStatus().getDescription()).contains("Authentication failed");
              assertThat(errorCodeOf(sre))
                  .isEqualTo(AnalyticsErrorCode.AUTHENTICATION_FAILED.getCode());
            });
  }

  @Test
  void testBuiltInRoleDeletion_mapsToFailedPrecondition() {
    assertThatThrownBy(
            () ->
                stub.createCatalog(
                    CreateCatalogRequest.newBuilder()
                        .setCatalogName("built-in-role-deletion")
                        .build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.FAILED_PRECONDITION.getCode());
              assertThat(sre.getStatus().getDescription())
                  .contains("Built-in entity cannot be modified");
              assertThat(errorCodeOf(sre))
                  .isEqualTo(AnalyticsErrorCode.BUILT_IN_ENTITY_NOT_MODIFIABLE.getCode());
            });
  }

  @Test
  void testResourceUnderAclDelegation_mapsToFailedPrecondition() {
    assertThatThrownBy(
            () ->
                stub.createCatalog(
                    CreateCatalogRequest.newBuilder()
                        .setCatalogName("resource-under-acl-delegation")
                        .build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.FAILED_PRECONDITION.getCode());
              assertThat(sre.getStatus().getDescription())
                  .contains("Resource is under ACL delegation");
              assertThat(errorCodeOf(sre))
                  .isEqualTo(AnalyticsErrorCode.RESOURCE_UNDER_ACL_DELEGATION.getCode());
            });
  }

  @Test
  void testDataInconsistency_mapsToDataLoss() {
    assertThatThrownBy(
            () ->
                stub.createCatalog(
                    CreateCatalogRequest.newBuilder().setCatalogName("data-inconsistency").build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.DATA_LOSS.getCode());
              assertThat(sre.getStatus().getDescription())
                  .contains("Persisted data is inconsistent");
              assertThat(errorCodeOf(sre))
                  .isEqualTo(AnalyticsErrorCode.DATA_INCONSISTENCY.getCode());
            });
  }

  @Test
  void testScalarDbClusterUnavailable_mapsToUnavailable() {
    assertThatThrownBy(
            () ->
                stub.createCatalog(
                    CreateCatalogRequest.newBuilder()
                        .setCatalogName("scalardb-unavailable")
                        .build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.UNAVAILABLE.getCode());
              assertThat(sre.getStatus().getDescription()).contains("ScalarDB Cluster");
              assertThat(errorCodeOf(sre))
                  .isEqualTo(AnalyticsErrorCode.SCALARDB_CLUSTER_UNAVAILABLE.getCode());
            });
  }

  @Test
  void testAnalyticsDbTransactionConflict_mapsToUnavailable() {
    // UNAVAILABLE is in the SDK's retryableStatusCodes, so the gRPC transport retries the request.
    assertThatThrownBy(
            () ->
                stub.createCatalog(
                    CreateCatalogRequest.newBuilder()
                        .setCatalogName("db-transaction-conflict")
                        .build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.UNAVAILABLE.getCode());
              assertThat(sre.getStatus().getDescription()).contains("transaction conflict");
              assertThat(errorCodeOf(sre))
                  .isEqualTo(AnalyticsErrorCode.ANALYTICS_DB_TRANSACTION_CONFLICT.getCode());
            });
  }

  @Test
  void testInternalError_mapsToInternal() {
    assertThatThrownBy(
            () ->
                stub.createCatalog(
                    CreateCatalogRequest.newBuilder().setCatalogName("internal-error").build()))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException sre = (StatusRuntimeException) e;
              assertThat(sre.getStatus().getCode()).isEqualTo(Status.INTERNAL.getCode());
              assertThat(sre.getStatus().getDescription()).contains("Internal error");
              assertThat(errorCodeOf(sre)).isEqualTo(AnalyticsErrorCode.INTERNAL_ERROR.getCode());
            });
  }

  @Test
  void testSuccessfulOperation_doesNotThrow() {
    // Act & Assert
    CreateCatalogResponse response =
        stub.createCatalog(CreateCatalogRequest.newBuilder().setCatalogName("success").build());

    assertThat(response).isNotNull();
  }

  /** Extracts the {@code AnalyticsError} error code from the {@code grpc-status-details-bin}. */
  private static String errorCodeOf(StatusRuntimeException sre) {
    return analyticsErrorOf(sre).getErrorCode();
  }

  /** Extracts the {@code AnalyticsError} detail from the {@code grpc-status-details-bin}. */
  static AnalyticsError analyticsErrorOf(StatusRuntimeException sre) {
    com.google.rpc.Status rpcStatus = StatusProto.fromThrowable(sre);
    assertThat(rpcStatus).isNotNull();
    for (Any detail : rpcStatus.getDetailsList()) {
      if (detail.is(AnalyticsError.class)) {
        try {
          return detail.unpack(AnalyticsError.class);
        } catch (InvalidProtocolBufferException e) {
          throw new AssertionError(e);
        }
      }
    }
    throw new AssertionError("No AnalyticsError detail found in status");
  }

  @Test
  void mapToGrpcStatus_ShouldMapEveryRetryableCodeToRetryableStatus() {
    // Guards #496: a code declared retryable must surface as a gRPC-retryable status (UNAVAILABLE),
    // so the gRPC transport actually retries it. Keeps the category and status axes consistent.
    for (AnalyticsErrorCode code : AnalyticsErrorCode.values()) {
      if (code.getErrorCategory() == AnalyticsErrorCode.ErrorCategory.RETRYABLE_SERVER_ERROR) {
        assertThat(ExceptionHandlingInterceptor.mapToGrpcStatus(code).getCode())
            .as("retryable code %s must map to UNAVAILABLE", code)
            .isEqualTo(Status.UNAVAILABLE.getCode());
      }
    }
  }

  @Test
  void mapToGrpcStatus_ShouldMapInvalidSchemaMetadataToInvalidArgument() {
    assertThat(
            ExceptionHandlingInterceptor.mapToGrpcStatus(AnalyticsErrorCode.INVALID_SCHEMA_METADATA)
                .getCode())
        .isEqualTo(Status.INVALID_ARGUMENT.getCode());
  }

  @Test
  void mapToGrpcStatus_ShouldMapNotFoundCodesToNotFound() {
    Set<AnalyticsErrorCode> notFoundCodes =
        EnumSet.of(
            AnalyticsErrorCode.CATALOG_NOT_FOUND,
            AnalyticsErrorCode.DATA_SOURCE_NOT_FOUND,
            AnalyticsErrorCode.NAMESPACE_NOT_FOUND,
            AnalyticsErrorCode.TABLE_NOT_FOUND,
            AnalyticsErrorCode.ROLE_NOT_FOUND,
            AnalyticsErrorCode.USER_NOT_FOUND,
            AnalyticsErrorCode.BACKEND_USER_NOT_FOUND,
            AnalyticsErrorCode.BACKEND_USER_LINK_NOT_FOUND);
    for (AnalyticsErrorCode code : AnalyticsErrorCode.values()) {
      boolean mapsToNotFound =
          code.getErrorCategory() != AnalyticsErrorCode.ErrorCategory.CLIENT_ERROR
              && ExceptionHandlingInterceptor.mapToGrpcStatus(code).getCode()
                  == Status.Code.NOT_FOUND;
      assertThat(mapsToNotFound)
          .as("%s maps to NOT_FOUND", code)
          .isEqualTo(notFoundCodes.contains(code));
    }
  }

  @Test
  void mapToGrpcStatus_ShouldMapBackendUserAlreadyLinkedToAlreadyExists() {
    assertThat(
            ExceptionHandlingInterceptor.mapToGrpcStatus(
                    AnalyticsErrorCode.BACKEND_USER_ALREADY_LINKED)
                .getCode())
        .isEqualTo(Status.ALREADY_EXISTS.getCode());
  }

  @Test
  void mapToGrpcStatus_ShouldMapInternalPasswordBackendRequiredToFailedPrecondition() {
    assertThat(
            ExceptionHandlingInterceptor.mapToGrpcStatus(
                    AnalyticsErrorCode.INTERNAL_PASSWORD_BACKEND_REQUIRED)
                .getCode())
        .isEqualTo(Status.FAILED_PRECONDITION.getCode());
  }

  @Test
  void mapToGrpcStatus_ShouldRejectClientErrorCodes() {
    // CLIENT_ERROR codes are produced only by the SDK; the server must never map them to a status.
    for (AnalyticsErrorCode code : AnalyticsErrorCode.values()) {
      if (code.getErrorCategory() == AnalyticsErrorCode.ErrorCategory.CLIENT_ERROR) {
        assertThatThrownBy(() -> ExceptionHandlingInterceptor.mapToGrpcStatus(code))
            .isInstanceOf(IllegalStateException.class);
      }
    }
  }

  // Test service that throws different exceptions based on catalog name
  private static class TestCatalogServiceImpl extends CatalogServiceGrpc.CatalogServiceImplBase {
    @Override
    public void createCatalog(
        CreateCatalogRequest request, StreamObserver<CreateCatalogResponse> responseObserver) {
      String catalogName = request.getCatalogName();

      switch (catalogName) {
        case "access-denied":
          throw new AnalyticsException(AnalyticsErrorCode.ACCESS_DENIED);

        case "deletion-blocked":
          throw new AnalyticsException(AnalyticsErrorCode.DATA_SOURCE_NOT_EMPTY);

        case "connection-error":
          throw new AnalyticsException(AnalyticsErrorCode.ANALYTICS_DB_CONNECTION_FAILED);

        case "already-exists":
          throw new AnalyticsException(AnalyticsErrorCode.CATALOG_ALREADY_EXISTS);

        case "constraint-violation":
          throw new AnalyticsException(AnalyticsErrorCode.CATALOG_NOT_EMPTY);

        case "database-error":
          throw new AnalyticsException(AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED);

        case "no-error-details":
          throw new AnalyticsException(AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED);

        case "schema-error":
          throw new AnalyticsException(AnalyticsErrorCode.DATA_SOURCE_UNREACHABLE);

        case "illegal-argument":
          throw new IllegalArgumentException("Invalid catalog name");

        case "privilege-check-unavailable":
          throw new AnalyticsException(AnalyticsErrorCode.SCALARDB_PRIVILEGE_CHECK_FAILURE);

        case "privilege-check-other":
          throw new AnalyticsException(AnalyticsErrorCode.SCALARDB_PRIVILEGE_CHECK_FAILURE);

        case "invalid-credentials":
          throw new AnalyticsException(AnalyticsErrorCode.AUTHENTICATION_FAILED);

        case "built-in-role-deletion":
          throw new AnalyticsException(AnalyticsErrorCode.BUILT_IN_ENTITY_NOT_MODIFIABLE);

        case "resource-under-acl-delegation":
          throw new AnalyticsException(AnalyticsErrorCode.RESOURCE_UNDER_ACL_DELEGATION);

        case "data-inconsistency":
          throw new AnalyticsException(AnalyticsErrorCode.DATA_INCONSISTENCY);

        case "scalardb-unavailable":
          throw new AnalyticsException(AnalyticsErrorCode.SCALARDB_CLUSTER_UNAVAILABLE);

        case "db-transaction-conflict":
          throw new AnalyticsException(AnalyticsErrorCode.ANALYTICS_DB_TRANSACTION_CONFLICT);

        case "internal-error":
          throw new AnalyticsException(AnalyticsErrorCode.INTERNAL_ERROR);

        case "role-not-found":
          throw new AnalyticsException(
              AnalyticsErrorCode.ROLE_NOT_FOUND, Map.of("role_name", "analyst"));

        case "blank-role-name":
          throw new AnalyticsException(
              AnalyticsErrorCode.INVALID_ARGUMENT, Map.of("field", "role_name", "reason", "blank"));

        case "generic-error":
          throw new RuntimeException("Unexpected error");

        case "success":
          responseObserver.onNext(CreateCatalogResponse.getDefaultInstance());
          responseObserver.onCompleted();
          break;

        default:
          throw new IllegalStateException("Unknown test case: " + catalogName);
      }
    }
  }
}
