/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.model.Column;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTableDetail;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.TableDetail;
import com.scalar.db.analytics.api.model.TableInfo;
import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider;
import com.scalar.db.analytics.client.cli.CliTestBase;
import com.scalar.db.analytics.sdk.table.TableClient;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

class DescribeTest extends CliTestBase {
  private final UUID dataSourceId = UUID.randomUUID();
  private final UUID catalogId = UUID.randomUUID();
  private final DataSourceProvider dataSourceProvider =
      new ScalarDbProvider(Collections.singletonMap("scalar.db.storage", "cassandra"));
  private final DataSource dataSource =
      new DataSource(dataSourceId, catalogId, "test_datasource", dataSourceProvider);

  private final UUID namespaceId = UUID.randomUUID();
  private final Namespace namespace =
      new Namespace(namespaceId, dataSourceId, Collections.singletonList("test_namespace"));

  private final UUID tableId = UUID.randomUUID();
  private final TableInfo tableInfo = new TableInfo(tableId, namespaceId, "test_table");
  private final TableDetail tableDetail =
      new TableDetail(tableInfo, Collections.<Column>emptyList());

  private final DataSourceNamespaceTableDetail dataSourceNamespaceTableDetail =
      new DataSourceNamespaceTableDetail(dataSource, namespace, tableDetail);

  @Nested
  class ByNameParameters {
    @Test
    void givenAllRequiredParams_whenFound_thenReturnTableDetail() {
      TableClient useCaseMock = modules.client().table();
      when(useCaseMock.describeTableByName(any(), any(), any(), any()))
          .thenReturn(Optional.of(dataSourceNamespaceTableDetail));

      int exitCode =
          new CommandLine(cliRoot)
              .execute(
                  args(
                      "table",
                      "describe",
                      "-c",
                      "test_catalog",
                      "-d",
                      "test_datasource",
                      "-n",
                      "test_namespace",
                      "-t",
                      "test_table"));
      assertThat(exitCode).isEqualTo(ExitCode.OK);
      assertThat(printer.getOutput()).contains("\"message\":\"Success\"").contains("test_table");
      verify(useCaseMock)
          .describeTableByName(
              "test_catalog",
              "test_datasource",
              Collections.singletonList("test_namespace"),
              "test_table");
      verifyNoMoreInteractions(useCaseMock);
    }

    @Test
    void givenAllRequiredParams_whenNotFound_thenReturnNotFoundError() {
      TableClient useCaseMock = modules.client().table();
      when(useCaseMock.describeTableByName(any(), any(), any(), any()))
          .thenReturn(Optional.empty());

      int exitCode =
          new CommandLine(cliRoot)
              .execute(
                  args(
                      "table",
                      "describe",
                      "-c",
                      "test_catalog",
                      "-d",
                      "test_datasource",
                      "-n",
                      "test_namespace",
                      "-t",
                      "test_table"));
      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(printer.getOutput())
          .contains("test_catalog.test_datasource.test_namespace.test_table");
      verify(useCaseMock)
          .describeTableByName(
              "test_catalog",
              "test_datasource",
              Collections.singletonList("test_namespace"),
              "test_table");
      verifyNoMoreInteractions(useCaseMock);
    }

    @Test
    void givenOnlyCatalogName_thenFails() {
      int exitCode =
          new CommandLine(cliRoot).execute(args("table", "describe", "--catalog", "test_catalog"));
      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Missing required argument");
      verifyNoInteractions(modules.client().table());
    }

    @Test
    void givenOnlyDataSourceName_thenFails() {
      int exitCode =
          new CommandLine(cliRoot)
              .execute(args("table", "describe", "--data-source", "test_datasource"));
      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Missing required argument");
      verifyNoInteractions(modules.client().table());
    }

    @Test
    void givenOnlyNamespaceName_thenFails() {
      int exitCode =
          new CommandLine(cliRoot)
              .execute(args("table", "describe", "--namespace", "test_namespace"));
      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Missing required argument");
      verifyNoInteractions(modules.client().table());
    }

    @Test
    void givenOnlyTableName_thenFails() {
      int exitCode =
          new CommandLine(cliRoot).execute(args("table", "describe", "--table", "test_table"));
      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Missing required argument");
      verifyNoInteractions(modules.client().table());
    }

    @Test
    void givenCatalogAndDataSource_whenMissingNamespaceAndTable_thenFails() {
      int exitCode =
          new CommandLine(cliRoot)
              .execute(args("table", "describe", "-c", "test_catalog", "-d", "test_datasource"));
      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Missing required argument");
      verifyNoInteractions(modules.client().table());
    }

    @Test
    void givenCatalogDataSourceAndNamespace_whenMissingTable_thenFails() {
      int exitCode =
          new CommandLine(cliRoot)
              .execute(
                  args(
                      "table",
                      "describe",
                      "-c",
                      "test_catalog",
                      "-d",
                      "test_datasource",
                      "-n",
                      "test_namespace"));
      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Missing required argument");
      verifyNoInteractions(modules.client().table());
    }

    @Test
    void givenCatalogDataSourceAndTable_whenMissingNamespace_thenFails() {
      int exitCode =
          new CommandLine(cliRoot)
              .execute(
                  args(
                      "table",
                      "describe",
                      "-c",
                      "test_catalog",
                      "-d",
                      "test_datasource",
                      "-t",
                      "test_table"));
      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Missing required argument");
      verifyNoInteractions(modules.client().table());
    }

    @Test
    void givenCatalogNamespaceAndTable_whenMissingDataSource_thenFails() {
      int exitCode =
          new CommandLine(cliRoot)
              .execute(
                  args(
                      "table",
                      "describe",
                      "-c",
                      "test_catalog",
                      "-n",
                      "test_namespace",
                      "-t",
                      "test_table"));
      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Missing required argument");
      verifyNoInteractions(modules.client().table());
    }

    @Test
    void givenDataSourceNamespaceAndTable_whenMissingCatalog_thenFails() {
      int exitCode =
          new CommandLine(cliRoot)
              .execute(
                  args(
                      "table",
                      "describe",
                      "-d",
                      "test_datasource",
                      "-n",
                      "test_namespace",
                      "-t",
                      "test_table"));
      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Missing required argument");
      verifyNoInteractions(modules.client().table());
    }
  }

  @Nested
  class ByIdParameters {
    @Test
    void givenTableId_whenFound_thenReturnTableDetail() {
      TableClient useCaseMock = modules.client().table();
      when(useCaseMock.describeTableById(any()))
          .thenReturn(Optional.of(dataSourceNamespaceTableDetail));

      int exitCode =
          new CommandLine(cliRoot).execute(args("table", "describe", "-i", tableId.toString()));
      assertThat(exitCode).isEqualTo(ExitCode.OK);
      assertThat(printer.getOutput()).contains("\"message\":\"Success\"").contains("test_table");
      verify(useCaseMock).describeTableById(tableId);
      verifyNoMoreInteractions(useCaseMock);
    }

    @Test
    void givenTableId_whenNotFound_thenReturnNotFoundError() {
      TableClient useCaseMock = modules.client().table();
      when(useCaseMock.describeTableById(tableId)).thenReturn(Optional.empty());

      int exitCode =
          new CommandLine(cliRoot).execute(args("table", "describe", "-i", tableId.toString()));
      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(printer.getOutput()).contains("Table was not found");
      verify(useCaseMock).describeTableById(tableId);
      verifyNoMoreInteractions(useCaseMock);
    }

    @Test
    void givenInvalidUuidFormat_thenFails() {
      int exitCode =
          new CommandLine(cliRoot)
              .execute(args("table", "describe", "--table-id", "not-a-valid-uuid"));

      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Invalid value for option '--table-id'");
      verifyNoInteractions(modules.client().table());
    }
  }

  @Nested
  class MutualExclusivity {
    @Test
    void givenBothByNameAndByIdParams_thenFails() {
      int exitCode =
          new CommandLine(cliRoot)
              .execute(
                  args(
                      "table",
                      "describe",
                      "-c",
                      "test_catalog",
                      "-d",
                      "test_datasource",
                      "-n",
                      "test_namespace",
                      "-t",
                      "test_table",
                      "-i",
                      tableId.toString()));
      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Error: expected only one match");
      verifyNoInteractions(modules.client().table());
    }
  }

  @Nested
  class NoParameters {
    @Test
    void givenNoParams_thenFails() {
      int exitCode = new CommandLine(cliRoot).execute(args("table", "describe"));
      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Missing required argument");
      verifyNoInteractions(modules.client().table());
    }
  }
}
