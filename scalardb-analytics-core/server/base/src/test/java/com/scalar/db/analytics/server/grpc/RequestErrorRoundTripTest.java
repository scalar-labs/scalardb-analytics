/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.protobuf.InvalidProtocolBufferException;
import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.datasource.provider.DynamoDbProvider;
import com.scalar.db.analytics.grpc.generated.auth.v1.CreateRoleRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.CreateUserWithBackendUserRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteRoleByIdRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteUserByIdRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.DescribeUserByIdRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantCatalogPermissionRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantPermissionByIdRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantRoleByIdRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantRoleRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.GranteeType;
import com.scalar.db.analytics.grpc.generated.auth.v1.LinkBackendUserRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListPermissionsByIdRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListPermissionsForRoleByIdRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.PasswordBackend;
import com.scalar.db.analytics.grpc.generated.auth.v1.PermissionServiceGrpc;
import com.scalar.db.analytics.grpc.generated.auth.v1.PermissionType;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokePermissionByIdRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeRoleByIdRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.RoleServiceGrpc;
import com.scalar.db.analytics.grpc.generated.auth.v1.UnlinkBackendUserRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.UserServiceGrpc;
import com.scalar.db.analytics.grpc.generated.catalog.v1.CatalogServiceGrpc;
import com.scalar.db.analytics.grpc.generated.catalog.v1.DeleteCatalogByIdRequest;
import com.scalar.db.analytics.grpc.generated.catalog.v1.FindCatalogByIdRequest;
import com.scalar.db.analytics.grpc.generated.catalog.v1.InitializeCatalogRequest;
import com.scalar.db.analytics.grpc.generated.datasource.v1.DataSourceServiceGrpc;
import com.scalar.db.analytics.grpc.generated.datasource.v1.DeleteDataSourceByIdRequest;
import com.scalar.db.analytics.grpc.generated.datasource.v1.FindDataSourceByIdRequest;
import com.scalar.db.analytics.grpc.generated.datasource.v1.RegisterRequest;
import com.scalar.db.analytics.grpc.generated.error.v1.AnalyticsError;
import com.scalar.db.analytics.grpc.generated.namespace.v1.DescribeNamespaceByIdRequest;
import com.scalar.db.analytics.grpc.generated.namespace.v1.NamespaceServiceGrpc;
import com.scalar.db.analytics.grpc.generated.table.v1.DescribeTableByIdRequest;
import com.scalar.db.analytics.grpc.generated.table.v1.TableServiceGrpc;
import com.scalar.db.analytics.grpc.mapper.datasource.DataSourceProviderJsonMapper;
import com.scalar.db.analytics.sdk.exception.GrpcExceptionMapper;
import com.scalar.db.analytics.usecase.CatalogUseCase;
import com.scalar.db.analytics.usecase.DataSourceUseCase;
import com.scalar.db.analytics.usecase.NamespaceUseCase;
import com.scalar.db.analytics.usecase.TableUseCase;
import com.scalar.db.analytics.usecase.auth.UserUseCase;
import com.scalar.db.analytics.usecase.authz.PermissionUseCase;
import com.scalar.db.analytics.usecase.authz.RoleUseCase;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.protobuf.StatusProto;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Sends failing requests through the real service implementations and {@link
 * ExceptionHandlingInterceptor}, and checks the gRPC status, the {@code AnalyticsError} detail, and
 * the error that the SDK reconstructs from it.
 *
 * <p>Requests whose fields do not decode fail in the gRPC layer without reaching a use case. For
 * failures detected in the use cases (not found, validation, conflicts, preconditions), the mocked
 * use case throws the exception the real one throws, which is covered by the use-case tests.
 */
class RequestErrorRoundTripTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final String VALID_ID = UUID.randomUUID().toString();
  private static final String MALFORMED_ID = "not-a-uuid";

  private final CatalogUseCase catalogUseCase = mock(CatalogUseCase.class);
  private final DataSourceUseCase dataSourceUseCase = mock(DataSourceUseCase.class);
  private final NamespaceUseCase namespaceUseCase = mock(NamespaceUseCase.class);
  private final TableUseCase tableUseCase = mock(TableUseCase.class);
  private final RoleUseCase roleUseCase = mock(RoleUseCase.class);
  private final UserUseCase userUseCase = mock(UserUseCase.class);
  private final PermissionUseCase permissionUseCase = mock(PermissionUseCase.class);

  private Server server;
  private ManagedChannel channel;

  @BeforeEach
  void setUp() throws IOException {
    String serverName = InProcessServerBuilder.generateName();
    server =
        InProcessServerBuilder.forName(serverName)
            .addService(new CatalogServiceImpl(catalogUseCase))
            .addService(new DataSourceServiceImpl(dataSourceUseCase))
            .addService(new NamespaceServiceImpl(namespaceUseCase))
            .addService(new TableServiceImpl(tableUseCase))
            .addService(new RoleServiceImpl(roleUseCase))
            .addService(new UserServiceImpl(userUseCase))
            .addService(new PermissionServiceImpl(permissionUseCase))
            // The last interceptor added runs first, as in CatalogGrpcServer.
            .intercept(userIdInterceptor())
            .intercept(new ExceptionHandlingInterceptor())
            .build()
            .start();
    channel = InProcessChannelBuilder.forName(serverName).build();
  }

  @AfterEach
  void tearDown() {
    channel.shutdownNow();
    server.shutdownNow();
  }

  /** A call, the request field it gets wrong, and the expected reason. */
  private record Case(String name, String field, String reason, Consumer<ManagedChannel> call) {
    @Override
    public String toString() {
      return name;
    }
  }

  static Stream<Arguments> malformedIds() {
    return Stream.of(
            new Case(
                "FindCatalogById",
                "catalog_id",
                "malformed_uuid",
                ch ->
                    CatalogServiceGrpc.newBlockingStub(ch)
                        .findCatalogById(
                            FindCatalogByIdRequest.newBuilder()
                                .setCatalogId(MALFORMED_ID)
                                .build())),
            new Case(
                "DeleteCatalogById",
                "catalog_id",
                "malformed_uuid",
                ch ->
                    CatalogServiceGrpc.newBlockingStub(ch)
                        .deleteCatalogById(
                            DeleteCatalogByIdRequest.newBuilder()
                                .setCatalogId(MALFORMED_ID)
                                .build())),
            new Case(
                "FindDataSourceById",
                "data_source_id",
                "malformed_uuid",
                ch ->
                    DataSourceServiceGrpc.newBlockingStub(ch)
                        .findDataSourceById(
                            FindDataSourceByIdRequest.newBuilder()
                                .setDataSourceId(MALFORMED_ID)
                                .build())),
            new Case(
                "DeleteDataSourceById",
                "data_source_id",
                "malformed_uuid",
                ch ->
                    DataSourceServiceGrpc.newBlockingStub(ch)
                        .deleteDataSourceById(
                            DeleteDataSourceByIdRequest.newBuilder()
                                .setDataSourceId(MALFORMED_ID)
                                .build())),
            new Case(
                "DescribeNamespaceById",
                "namespace_id",
                "malformed_uuid",
                ch ->
                    NamespaceServiceGrpc.newBlockingStub(ch)
                        .describeNamespaceById(
                            DescribeNamespaceByIdRequest.newBuilder()
                                .setNamespaceId(MALFORMED_ID)
                                .build())),
            new Case(
                "DescribeTableById",
                "table_id",
                "malformed_uuid",
                ch ->
                    TableServiceGrpc.newBlockingStub(ch)
                        .describeTableById(
                            DescribeTableByIdRequest.newBuilder()
                                .setTableId(MALFORMED_ID)
                                .build())),
            new Case(
                "DeleteRoleById",
                "role_id",
                "malformed_uuid",
                ch ->
                    RoleServiceGrpc.newBlockingStub(ch)
                        .deleteRoleById(
                            DeleteRoleByIdRequest.newBuilder().setRoleId(MALFORMED_ID).build())),
            new Case(
                "GrantRoleById (role_id)",
                "role_id",
                "malformed_uuid",
                ch ->
                    RoleServiceGrpc.newBlockingStub(ch)
                        .grantRoleById(
                            GrantRoleByIdRequest.newBuilder()
                                .setRoleId(MALFORMED_ID)
                                .setUserId(VALID_ID)
                                .build())),
            new Case(
                "RevokeRoleById (user_id)",
                "user_id",
                "malformed_uuid",
                ch ->
                    RoleServiceGrpc.newBlockingStub(ch)
                        .revokeRoleById(
                            RevokeRoleByIdRequest.newBuilder()
                                .setRoleId(VALID_ID)
                                .setUserId(MALFORMED_ID)
                                .build())),
            new Case(
                "DescribeUserById",
                "user_id",
                "malformed_uuid",
                ch ->
                    UserServiceGrpc.newBlockingStub(ch)
                        .describeUserById(
                            DescribeUserByIdRequest.newBuilder().setUserId(MALFORMED_ID).build())),
            new Case(
                "DeleteUserById (empty)",
                "user_id",
                "malformed_uuid",
                ch ->
                    UserServiceGrpc.newBlockingStub(ch)
                        .deleteUserById(DeleteUserByIdRequest.newBuilder().setUserId("").build())),
            new Case(
                "GrantPermissionById (grantee_id)",
                "grantee_id",
                "malformed_uuid",
                ch ->
                    PermissionServiceGrpc.newBlockingStub(ch)
                        .grantPermissionById(
                            GrantPermissionByIdRequest.newBuilder()
                                .setGranteeType(GranteeType.GRANTEE_TYPE_USER)
                                .setGranteeId(MALFORMED_ID)
                                .setPermission(PermissionType.PERMISSION_TYPE_CATALOG_READ)
                                .setResourceId(VALID_ID)
                                .build())),
            new Case(
                "RevokePermissionById (resource_id)",
                "resource_id",
                "malformed_uuid",
                ch ->
                    PermissionServiceGrpc.newBlockingStub(ch)
                        .revokePermissionById(
                            RevokePermissionByIdRequest.newBuilder()
                                .setGranteeType(GranteeType.GRANTEE_TYPE_ROLE)
                                .setGranteeId(VALID_ID)
                                .setPermission(PermissionType.PERMISSION_TYPE_CATALOG_READ)
                                .setResourceId(MALFORMED_ID)
                                .build())),
            new Case(
                "ListPermissionsById",
                "user_id",
                "malformed_uuid",
                ch ->
                    PermissionServiceGrpc.newBlockingStub(ch)
                        .listPermissionsById(
                            ListPermissionsByIdRequest.newBuilder()
                                .setUserId(MALFORMED_ID)
                                .build())),
            new Case(
                "ListPermissionsForRoleById",
                "role_id",
                "malformed_uuid",
                ch ->
                    PermissionServiceGrpc.newBlockingStub(ch)
                        .listPermissionsForRoleById(
                            ListPermissionsForRoleByIdRequest.newBuilder()
                                .setRoleId(MALFORMED_ID)
                                .build())))
        .map(Arguments::of);
  }

  static Stream<Arguments> unspecifiedOrUnrecognizedEnums() {
    return Stream.of(
            new Case(
                "GrantCatalogPermission (grantee_type unspecified)",
                "grantee_type",
                "unspecified",
                ch ->
                    PermissionServiceGrpc.newBlockingStub(ch)
                        .grantCatalogPermission(
                            GrantCatalogPermissionRequest.newBuilder()
                                .setGranteeName("alice")
                                .setPermission(PermissionType.PERMISSION_TYPE_CATALOG_READ)
                                .setCatalogName("cat")
                                .build())),
            new Case(
                "GrantCatalogPermission (grantee_type unrecognized)",
                "grantee_type",
                "unrecognized",
                ch ->
                    PermissionServiceGrpc.newBlockingStub(ch)
                        .grantCatalogPermission(
                            GrantCatalogPermissionRequest.newBuilder()
                                .setGranteeTypeValue(99)
                                .setGranteeName("alice")
                                .setPermission(PermissionType.PERMISSION_TYPE_CATALOG_READ)
                                .setCatalogName("cat")
                                .build())),
            new Case(
                "GrantCatalogPermission (permission unspecified)",
                "permission",
                "unspecified",
                ch ->
                    PermissionServiceGrpc.newBlockingStub(ch)
                        .grantCatalogPermission(
                            GrantCatalogPermissionRequest.newBuilder()
                                .setGranteeType(GranteeType.GRANTEE_TYPE_USER)
                                .setGranteeName("alice")
                                .setCatalogName("cat")
                                .build())),
            new Case(
                "GrantPermissionById (permission unrecognized)",
                "permission",
                "unrecognized",
                ch ->
                    PermissionServiceGrpc.newBlockingStub(ch)
                        .grantPermissionById(
                            GrantPermissionByIdRequest.newBuilder()
                                .setGranteeType(GranteeType.GRANTEE_TYPE_USER)
                                .setGranteeId(VALID_ID)
                                .setPermissionValue(99)
                                .setResourceId(VALID_ID)
                                .build())),
            new Case(
                "LinkBackendUser (backend unspecified)",
                "backend",
                "unspecified",
                ch ->
                    UserServiceGrpc.newBlockingStub(ch)
                        .linkBackendUser(
                            LinkBackendUserRequest.newBuilder()
                                .setUsername("alice")
                                .setBackendUsername("alice")
                                .build())),
            new Case(
                "UnlinkBackendUser (backend unrecognized)",
                "backend",
                "unrecognized",
                ch ->
                    UserServiceGrpc.newBlockingStub(ch)
                        .unlinkBackendUser(
                            UnlinkBackendUserRequest.newBuilder()
                                .setUsername("alice")
                                .setBackendUsername("alice")
                                .setBackendValue(99)
                                .build())))
        .map(Arguments::of);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource({"malformedIds", "unspecifiedOrUnrecognizedEnums"})
  void undecodableField_shouldFailWithInvalidArgumentNamingTheField(Case c) {
    StatusRuntimeException e =
        catchThrowableOfType(StatusRuntimeException.class, () -> c.call().accept(channel));

    assertInvalidArgument(e, Map.of("field", c.field(), "reason", c.reason()));
    verifyNoUseCaseCalled();
  }

  @Test
  void register_withEmptyProvider_shouldFailWithProviderRequired() {
    StatusRuntimeException e =
        catchThrowableOfType(
            StatusRuntimeException.class,
            () ->
                DataSourceServiceGrpc.newBlockingStub(channel)
                    .register(registerRequest().clearProviderPayloadJson().build()));

    assertInvalidArgument(
        e,
        Map.of(
            "catalog_name", "cat",
            "data_source_name", "ds",
            "field", "provider_payload_json",
            "reason", "required"));
    verifyNoUseCaseCalled();
  }

  @Test
  void register_withUnknownProviderType_shouldFailWithProviderInvalid() {
    StatusRuntimeException e =
        catchThrowableOfType(
            StatusRuntimeException.class,
            () ->
                DataSourceServiceGrpc.newBlockingStub(channel)
                    .register(
                        registerRequest()
                            .setProviderPayloadJson("{\"type\":\"unknown\"}")
                            .build()));

    assertInvalidArgument(
        e,
        Map.of(
            "catalog_name", "cat",
            "data_source_name", "ds",
            "field", "provider_payload_json",
            "reason", "invalid"));
    verifyNoUseCaseCalled();
  }

  @Test
  void register_withProviderRejectedByItsBuilder_shouldFailWithProviderInvalid() {
    // DynamoDbProvider.Builder requires a region or an endpoint.
    StatusRuntimeException e =
        catchThrowableOfType(
            StatusRuntimeException.class,
            () ->
                DataSourceServiceGrpc.newBlockingStub(channel)
                    .register(
                        registerRequest()
                            .setProviderPayloadJson("{\"type\":\"dynamodb\"}")
                            .build()));

    assertInvalidArgument(
        e,
        Map.of(
            "catalog_name", "cat",
            "data_source_name", "ds",
            "field", "provider_payload_json",
            "reason", "invalid"));
    verifyNoUseCaseCalled();
  }

  @Test
  void register_withMalformedSchema_shouldFailWithSchemaInvalid() {
    StatusRuntimeException e =
        catchThrowableOfType(
            StatusRuntimeException.class,
            () ->
                DataSourceServiceGrpc.newBlockingStub(channel)
                    .register(registerRequest().setSchemaJson("{not json").build()));

    assertInvalidArgument(
        e,
        Map.of(
            "catalog_name", "cat",
            "data_source_name", "ds",
            "field", "schema_json",
            "reason", "invalid"));
    verifyNoUseCaseCalled();
  }

  @Test
  void register_withUnknownColumnType_shouldFailWithSchemaInvalid() {
    String schema =
        "{\"namespaces\":[{\"names\":[\"ns\"],\"tables\":[{\"name\":\"t\",\"columns\":"
            + "[{\"name\":\"c\",\"type\":\"no_such_type\"}]}]}]}";
    StatusRuntimeException e =
        catchThrowableOfType(
            StatusRuntimeException.class,
            () ->
                DataSourceServiceGrpc.newBlockingStub(channel)
                    .register(registerRequest().setSchemaJson(schema).build()));

    assertInvalidArgument(
        e,
        Map.of(
            "catalog_name", "cat",
            "data_source_name", "ds",
            "field", "schema_json",
            "reason", "invalid"));
    verifyNoUseCaseCalled();
  }

  @Test
  void initializeCatalog_withUndecodableDataSource_shouldNameTheDataSource() {
    StatusRuntimeException e =
        catchThrowableOfType(
            StatusRuntimeException.class,
            () ->
                CatalogServiceGrpc.newBlockingStub(channel)
                    .initializeCatalog(
                        InitializeCatalogRequest.newBuilder()
                            .setCatalogName("cat")
                            .addDataSources(registerRequest())
                            .addDataSources(
                                registerRequest()
                                    .setDataSourceName("broken")
                                    .setProviderPayloadJson("[]"))
                            .build()));

    assertInvalidArgument(
        e,
        Map.of(
            "catalog_name", "cat",
            "data_source_name", "broken",
            "field", "provider_payload_json",
            "reason", "invalid"));
    verifyNoUseCaseCalled();
  }

  // ---------------- Failures detected in the use cases ----------------

  @Test
  void grantRole_withMissingRole_shouldFailWithNotFound() {
    Map<String, String> metadata = Map.of("role_name", "analyst");
    doThrow(new AnalyticsException(AnalyticsErrorCode.ROLE_NOT_FOUND, metadata))
        .when(roleUseCase)
        .grantRole(USER_ID, "analyst", "alice");

    StatusRuntimeException e =
        catchThrowableOfType(
            StatusRuntimeException.class,
            () ->
                RoleServiceGrpc.newBlockingStub(channel)
                    .grantRole(
                        GrantRoleRequest.newBuilder()
                            .setRoleName("analyst")
                            .setUsername("alice")
                            .build()));

    assertError(e, Status.Code.NOT_FOUND, AnalyticsErrorCode.ROLE_NOT_FOUND, metadata);
  }

  @Test
  void register_withMissingCatalog_shouldFailWithNotFound() {
    Map<String, String> metadata = Map.of("catalog_name", "cat");
    when(dataSourceUseCase.register(eq(USER_ID), any()))
        .thenThrow(new AnalyticsException(AnalyticsErrorCode.CATALOG_NOT_FOUND, metadata));

    StatusRuntimeException e =
        catchThrowableOfType(
            StatusRuntimeException.class,
            () ->
                DataSourceServiceGrpc.newBlockingStub(channel).register(registerRequest().build()));

    assertError(e, Status.Code.NOT_FOUND, AnalyticsErrorCode.CATALOG_NOT_FOUND, metadata);
  }

  @Test
  void createRole_withBlankName_shouldFailWithInvalidArgument() {
    Map<String, String> metadata = Map.of("field", "role_name", "reason", "blank");
    when(roleUseCase.createRole(USER_ID, " "))
        .thenThrow(new AnalyticsException(AnalyticsErrorCode.INVALID_ARGUMENT, metadata));

    StatusRuntimeException e =
        catchThrowableOfType(
            StatusRuntimeException.class,
            () ->
                RoleServiceGrpc.newBlockingStub(channel)
                    .createRole(CreateRoleRequest.newBuilder().setRoleName(" ").build()));

    assertInvalidArgument(e, metadata);
  }

  @Test
  void linkBackendUser_whenAlreadyLinked_shouldFailWithAlreadyExists() {
    Map<String, String> metadata =
        Map.of(
            "reason", "backend_user_linked",
            "username", "alice",
            "backend", "scalardb_cluster",
            "backend_username", "alice");
    doThrow(new AnalyticsException(AnalyticsErrorCode.BACKEND_USER_ALREADY_LINKED, metadata))
        .when(userUseCase)
        .linkBackendUser(USER_ID, "alice", "alice", PasswordBackendType.SCALARDB_CLUSTER);

    StatusRuntimeException e =
        catchThrowableOfType(
            StatusRuntimeException.class,
            () ->
                UserServiceGrpc.newBlockingStub(channel)
                    .linkBackendUser(
                        LinkBackendUserRequest.newBuilder()
                            .setUsername("alice")
                            .setBackendUsername("alice")
                            .setBackend(PasswordBackend.PASSWORD_BACKEND_SCALARDB_CLUSTER)
                            .build()));

    assertError(
        e, Status.Code.ALREADY_EXISTS, AnalyticsErrorCode.BACKEND_USER_ALREADY_LINKED, metadata);
  }

  @Test
  void createUserWithBackendUser_withoutInternalBackend_shouldFailWithFailedPrecondition() {
    when(userUseCase.createUserWithBackendUser(USER_ID, "alice", "alice", "pw"))
        .thenThrow(new AnalyticsException(AnalyticsErrorCode.INTERNAL_PASSWORD_BACKEND_REQUIRED));

    StatusRuntimeException e =
        catchThrowableOfType(
            StatusRuntimeException.class,
            () ->
                UserServiceGrpc.newBlockingStub(channel)
                    .createUserWithBackendUser(
                        CreateUserWithBackendUserRequest.newBuilder()
                            .setUsername("alice")
                            .setBackendUsername("alice")
                            .setPassword("pw")
                            .build()));

    assertError(
        e,
        Status.Code.FAILED_PRECONDITION,
        AnalyticsErrorCode.INTERNAL_PASSWORD_BACKEND_REQUIRED,
        Map.of());
  }

  @Test
  void unexpectedIllegalArgumentException_shouldFailWithInternal() {
    when(roleUseCase.createRole(USER_ID, "analyst"))
        .thenThrow(new IllegalArgumentException("a server-side defect"));

    StatusRuntimeException e =
        catchThrowableOfType(
            StatusRuntimeException.class,
            () ->
                RoleServiceGrpc.newBlockingStub(channel)
                    .createRole(CreateRoleRequest.newBuilder().setRoleName("analyst").build()));

    assertError(e, Status.Code.INTERNAL, AnalyticsErrorCode.INTERNAL_ERROR, Map.of());
  }

  private static RegisterRequest.Builder registerRequest() {
    return RegisterRequest.newBuilder()
        .setCatalogName("cat")
        .setDataSourceName("ds")
        .setProviderPayloadJson(
            DataSourceProviderJsonMapper.INSTANCE.toJson(
                DynamoDbProvider.builder().region("us-east-1").build()));
  }

  static void assertInvalidArgument(StatusRuntimeException e, Map<String, String> metadata) {
    assertError(e, Status.Code.INVALID_ARGUMENT, AnalyticsErrorCode.INVALID_ARGUMENT, metadata);
  }

  static void assertError(
      StatusRuntimeException e,
      Status.Code status,
      AnalyticsErrorCode code,
      Map<String, String> metadata) {
    // gRPC status and the AnalyticsError detail on the wire.
    assertThat(e).isNotNull();
    assertThat(e.getStatus().getCode()).isEqualTo(status);
    AnalyticsError detail = analyticsError(e);
    assertThat(detail.getErrorCode()).isEqualTo(code.getCode());
    assertThat(detail.getMetadataMap()).isEqualTo(metadata);

    // What an SDK caller sees.
    AnalyticsException sdkError = GrpcExceptionMapper.toAnalyticsException(e);
    assertThat(sdkError.getErrorCode()).isEqualTo(code);
    assertThat(sdkError.getMetadata()).containsAllEntriesOf(metadata);
  }

  private static AnalyticsError analyticsError(StatusRuntimeException e) {
    com.google.rpc.Status status = StatusProto.fromThrowable(e);
    assertThat(status).isNotNull();
    assertThat(status.getDetailsCount()).isEqualTo(1);
    try {
      return status.getDetails(0).unpack(AnalyticsError.class);
    } catch (InvalidProtocolBufferException ex) {
      throw new AssertionError(ex);
    }
  }

  private void verifyNoUseCaseCalled() {
    verifyNoInteractions(
        catalogUseCase,
        dataSourceUseCase,
        namespaceUseCase,
        tableUseCase,
        roleUseCase,
        userUseCase,
        permissionUseCase);
  }

  private static ServerInterceptor userIdInterceptor() {
    return new ServerInterceptor() {
      @Override
      public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
          ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        Context ctx =
            Context.current().withValue(AuthenticationInterceptor.AUTHENTICATED_USER_ID, USER_ID);
        return Contexts.interceptCall(ctx, call, headers, next);
      }
    };
  }
}
