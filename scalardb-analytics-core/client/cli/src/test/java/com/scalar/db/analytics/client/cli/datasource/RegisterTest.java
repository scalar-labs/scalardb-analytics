/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest;
import com.scalar.db.analytics.client.cli.CliTestBase;
import com.scalar.db.analytics.sdk.datasource.DataSourceClient;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

class RegisterTest extends CliTestBase {
  private final DataSource fakeDataSource =
      new DataSource(
          UUID.randomUUID(),
          UUID.randomUUID(),
          "test_data_source",
          new PostgreSql("localhost", 5432, "postgres", "password", "postgres"));

  // Set only by the tests that replace stdin, and reset back to null after each of them.
  @Nullable private InputStream originalIn;

  @AfterEach
  void resetStdin() {
    if (originalIn != null) {
      System.setIn(originalIn);
      originalIn = null;
    }
  }

  @Test
  void whenProviderJsonInline_thenRegisterSuccess() throws Exception {
    DataSourceClient useCaseMock = modules.client().dataSource();
    when(useCaseMock.register(any(RegisterDataSourceRequest.class))).thenReturn(fakeDataSource);

    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args(
                    "data-source",
                    "register",
                    "--catalog",
                    "test_catalog",
                    "--data-source",
                    "test_data_source",
                    "--provider-json",
                    "{\"type\":\"postgresql\",\"host\":\"localhost\",\"port\":5432,\"username\":\"postgres\",\"password\":\"password\",\"database\":\"postgres\"}\n"));
    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.toString()).contains("Success");
    ArgumentCaptor<RegisterDataSourceRequest> captor =
        ArgumentCaptor.forClass(RegisterDataSourceRequest.class);
    verify(useCaseMock).register(captor.capture());
    assertThat(captor.getValue().getSchema()).isNull();
  }

  @Test
  void whenProviderFile_thenRegisterSuccess(@TempDir Path tempDir) throws Exception {
    Path providerFile = tempDir.resolve("provider.json");
    Files.write(
        providerFile,
        ("{\n"
                + "  \"type\": \"postgresql\",\n"
                + "  \"host\": \"localhost\",\n"
                + "  \"port\": 5432,\n"
                + "  \"username\": \"postgres\",\n"
                + "  \"password\": \"password\",\n"
                + "  \"database\": \"postgres\"\n"
                + "}\n")
            .getBytes(StandardCharsets.UTF_8));

    DataSourceClient useCaseMock = modules.client().dataSource();
    when(useCaseMock.register(any(RegisterDataSourceRequest.class))).thenReturn(fakeDataSource);

    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args(
                    "data-source",
                    "register",
                    "--catalog",
                    "test_catalog",
                    "--data-source",
                    "test_data_source",
                    "--provider-file",
                    providerFile.toString()));
    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.toString()).contains("Success");
    ArgumentCaptor<RegisterDataSourceRequest> captor =
        ArgumentCaptor.forClass(RegisterDataSourceRequest.class);
    verify(useCaseMock).register(captor.capture());
    assertThat(captor.getValue().getSchema()).isNull();
  }

  @Test
  void whenProviderReadFromStdin_thenRegisterSuccess() throws Exception {
    DataSourceClient useCaseMock = modules.client().dataSource();
    when(useCaseMock.register(any(RegisterDataSourceRequest.class))).thenReturn(fakeDataSource);

    originalIn = System.in;
    System.setIn(
        new ByteArrayInputStream(
            ("{\"type\":\"postgresql\",\"host\":\"localhost\",\"port\":5432,\"username\":\"postgres\",\"password\":\"password\",\"database\":\"postgres\"}\n")
                .getBytes(StandardCharsets.UTF_8)));

    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args(
                    "data-source",
                    "register",
                    "--catalog",
                    "test_catalog",
                    "--data-source",
                    "test_data_source",
                    "--provider-stdin"));
    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.toString()).contains("Success");
    ArgumentCaptor<RegisterDataSourceRequest> captor =
        ArgumentCaptor.forClass(RegisterDataSourceRequest.class);
    verify(useCaseMock).register(captor.capture());
    assertThat(captor.getValue().getSchema()).isNull();
  }

  @Test
  void whenProviderFileMissing_thenError() {
    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args(
                    "data-source",
                    "register",
                    "--catalog",
                    "test_catalog",
                    "--data-source",
                    "test_data_source",
                    "--provider-file",
                    "/invalid/path.json"));

    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
    assertThat(printer.getOutput()).startsWith("Failed to read provider payload");
  }

  @Test
  void whenProviderJsonMalformed_thenError() {
    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args(
                    "data-source",
                    "register",
                    "--catalog",
                    "test_catalog",
                    "--data-source",
                    "test_data_source",
                    "--provider-json",
                    "not-json"));

    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
    assertThat(printer.getOutput()).contains("Failed to process provider payload");
  }

  @Test
  void whenSchemaProvidedForAutoProvider_thenError() {
    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args(
                    "data-source",
                    "register",
                    "--catalog",
                    "test_catalog",
                    "--data-source",
                    "test_data_source",
                    "--provider-json",
                    "{\"type\":\"postgresql\",\"host\":\"localhost\",\"port\":5432,\"username\":\"postgres\",\"password\":\"password\",\"database\":\"postgres\"}\n",
                    "--schema-json",
                    "{\"namespaces\":[{\"names\":[\"ns\"],\"tables\":[{\"name\":\"t\",\"columns\":[{\"name\":\"id\",\"type\":\"TEXT\",\"nullable\":false}]}]}]}\n"));

    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
    assertThat(printer.getOutput()).contains("resolves schemas automatically");
  }

  @Test
  void whenManualProviderWithoutSchema_thenError() {
    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args(
                    "data-source",
                    "register",
                    "--catalog",
                    "test_catalog",
                    "--data-source",
                    "test_data_source",
                    "--provider-json",
                    "{\"type\":\"dynamodb\",\"region\":\"us-east-1\"}\n"));

    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
    assertThat(printer.getOutput()).contains("requires a schema");
  }

  @Test
  void whenManualProviderWithSchema_thenRegisterSuccess() throws Exception {
    DataSourceClient useCaseMock = modules.client().dataSource();
    when(useCaseMock.register(any(RegisterDataSourceRequest.class))).thenReturn(fakeDataSource);

    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args(
                    "data-source",
                    "register",
                    "--catalog",
                    "test_catalog",
                    "--data-source",
                    "test_data_source",
                    "--provider-json",
                    "{\"type\":\"dynamodb\",\"region\":\"us-east-1\"}\n",
                    "--schema-json",
                    "{\"namespaces\":[{\"names\":[\"ns\"],\"tables\":[{\"name\":\"t\",\"columns\":[{\"name\":\"id\",\"type\":\"TEXT\",\"nullable\":false}]}]}]}\n"));

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.toString()).contains("Success");
    ArgumentCaptor<RegisterDataSourceRequest> captor =
        ArgumentCaptor.forClass(RegisterDataSourceRequest.class);
    verify(useCaseMock).register(captor.capture());
    assertThat(captor.getValue().getSchema()).isNotNull();
    assertThat(captor.getValue().getSchema().getNamespaces()).hasSize(1);
  }
}
