/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;
import com.scalar.db.analytics.client.cli.CliTestBase;
import com.scalar.db.analytics.sdk.datasource.DataSourceClient;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

class DescribeTest extends CliTestBase {
  private final UUID fakeCatalogId = UUID.randomUUID();
  private final UUID fakeDataSourceId = UUID.randomUUID();
  private final DataSourceProvider fakeDataSourceProvider =
      new PostgreSql("localhost", 5432, "postgres", "password", "postgres");
  private final DataSource fakeDataSource =
      new DataSource(fakeDataSourceId, fakeCatalogId, "test_data_source", fakeDataSourceProvider);

  @Test
  void givenNoCatalogName_thenFails() {
    int exitCode = new CommandLine(cliRoot).execute(args("catalog", "describe"));
    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
  }

  @Test
  void givenNoDataSourceName_thenFails() {
    int exitCode =
        new CommandLine(cliRoot).execute(args("catalog", "describe", "--catalog", "test_catalog"));
    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
  }

  @Test
  void whenFound_thenReturnDataSource() {
    DataSourceClient useCaseMock = modules.client().dataSource();
    when(useCaseMock.findDataSourceByName(any(), any())).thenReturn(Optional.of(fakeDataSource));

    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args(
                    "data-source",
                    "describe",
                    "--catalog",
                    "test_catalog",
                    "--data-source",
                    "test_data_source"));
    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.getOutput())
        .contains("\"message\":\"Success\"")
        .contains("\"name\":\"" + fakeDataSource.getName() + "\"");
    verify(useCaseMock).findDataSourceByName("test_catalog", "test_data_source");
    verifyNoMoreInteractions(useCaseMock);
  }

  @Test
  void whenNotFound_thenReturnNotFoundError() {
    DataSourceClient useCaseMock = modules.client().dataSource();
    when(useCaseMock.findDataSourceByName(any(), any())).thenReturn(Optional.empty());

    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args(
                    "data-source",
                    "describe",
                    "--catalog",
                    "test_catalog",
                    "--data-source",
                    "test_data_source"));
    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
    assertThat(printer.getOutput()).startsWith("Data source was not found");
  }

  @Test
  void givenBothByNameAndByIdParams_thenFails() {
    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args(
                    "data-source",
                    "describe",
                    "--catalog",
                    "test_catalog",
                    "--data-source",
                    "test_data_source",
                    "--data-source-id",
                    fakeDataSourceId.toString()));
    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
    assertThat(getStderr()).contains("Error: expected only one match but got");
  }

  @Test
  void givenDataSourceIdWithById_whenFound_thenReturnDataSource() {
    DataSourceClient useCaseMock = modules.client().dataSource();
    when(useCaseMock.findDataSourceById(fakeDataSourceId)).thenReturn(Optional.of(fakeDataSource));

    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args("data-source", "describe", "--data-source-id", fakeDataSourceId.toString()));

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.getOutput())
        .contains("\"message\":\"Success\"")
        .contains("\"name\":\"" + fakeDataSource.getName() + "\"");
    verify(useCaseMock).findDataSourceById(fakeDataSourceId);
    verifyNoMoreInteractions(useCaseMock);
  }

  @Test
  void givenDataSourceIdWithById_whenNotFound_thenReturnNotFoundError() {
    DataSourceClient useCaseMock = modules.client().dataSource();
    when(useCaseMock.findDataSourceById(fakeDataSourceId)).thenReturn(Optional.empty());

    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args("data-source", "describe", "--data-source-id", fakeDataSourceId.toString()));

    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
    assertThat(printer.getOutput()).startsWith("Data source was not found");
    verify(useCaseMock).findDataSourceById(fakeDataSourceId);
    verifyNoMoreInteractions(useCaseMock);
  }

  @Test
  void givenByIdWithoutDataSourceId_thenFails() {
    int exitCode = new CommandLine(cliRoot).execute(args("data-source", "describe"));

    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
    assertThat(getStderr()).contains("Error: Missing required argument");
  }

  @Test
  void givenInvalidUuidFormat_thenFails() {
    int exitCode =
        new CommandLine(cliRoot)
            .execute(args("data-source", "describe", "--data-source-id", "not-a-valid-uuid"));

    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
    assertThat(getStderr()).contains("Invalid value for option '--data-source-id'");
    verifyNoInteractions(modules.client().dataSource());
  }
}
