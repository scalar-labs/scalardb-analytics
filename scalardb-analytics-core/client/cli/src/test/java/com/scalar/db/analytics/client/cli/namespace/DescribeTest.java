/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.namespace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.client.cli.CliTestBase;
import com.scalar.db.analytics.sdk.namespace.NamespaceClient;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

class DescribeTest extends CliTestBase {
  private final UUID namespaceId = UUID.randomUUID();
  private final UUID dataSourceId = UUID.randomUUID();
  private final Namespace namespace =
      new Namespace(namespaceId, dataSourceId, Collections.singletonList("test_namespace"));

  @Nested
  class ByNameParameters {
    @Test
    void givenAllRequiredParams_whenFound_thenReturnNamespace() {
      NamespaceClient useCaseMock = modules.client().namespace();
      when(useCaseMock.findNamespaceByName(
              "test_catalog", "test_datasource", Collections.singletonList("test_namespace")))
          .thenReturn(Optional.of(namespace));

      int exitCode =
          new CommandLine(cliRoot)
              .execute(
                  args(
                      "namespace",
                      "describe",
                      "--catalog",
                      "test_catalog",
                      "--data-source",
                      "test_datasource",
                      "--namespace",
                      "test_namespace"));

      assertThat(exitCode).isEqualTo(ExitCode.OK);
      assertThat(printer.getOutput())
          .contains("\"message\":\"Success\"")
          .contains("test_namespace");
      verify(useCaseMock)
          .findNamespaceByName(
              "test_catalog", "test_datasource", Collections.singletonList("test_namespace"));
      verifyNoMoreInteractions(useCaseMock);
    }

    @Test
    void givenAllRequiredParams_whenNotFound_thenReturnNotFoundError() {
      NamespaceClient useCaseMock = modules.client().namespace();
      when(useCaseMock.findNamespaceByName(
              "test_catalog", "test_datasource", Collections.singletonList("test_namespace")))
          .thenReturn(Optional.empty());

      int exitCode =
          new CommandLine(cliRoot)
              .execute(
                  args(
                      "namespace",
                      "describe",
                      "--catalog",
                      "test_catalog",
                      "--data-source",
                      "test_datasource",
                      "--namespace",
                      "test_namespace"));

      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(printer.getOutput()).startsWith("Namespace was not found");
      verify(useCaseMock)
          .findNamespaceByName(
              "test_catalog", "test_datasource", Collections.singletonList("test_namespace"));
      verifyNoMoreInteractions(useCaseMock);
    }

    @Test
    void givenOnlyCatalogName_thenFails() {
      int exitCode =
          new CommandLine(cliRoot)
              .execute(args("namespace", "describe", "--catalog", "test_catalog"));
      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Error: Missing required argument");
      verifyNoInteractions(modules.client().namespace());
    }

    @Test
    void givenOnlyDataSourceName_thenFails() {
      int exitCode =
          new CommandLine(cliRoot)
              .execute(args("namespace", "describe", "--data-source", "test_datasource"));
      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Error: Missing required argument");
      verifyNoInteractions(modules.client().namespace());
    }

    @Test
    void givenOnlyNamespaceName_thenFails() {
      int exitCode =
          new CommandLine(cliRoot)
              .execute(args("namespace", "describe", "--namespace", "test_namespace"));
      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Error: Missing required argument");
      verifyNoInteractions(modules.client().namespace());
    }

    @Test
    void givenCatalogAndDataSource_whenMissingNamespace_thenFails() {
      int exitCode =
          new CommandLine(cliRoot)
              .execute(
                  args(
                      "namespace",
                      "describe",
                      "--catalog",
                      "test_catalog",
                      "--data-source",
                      "test_datasource"));
      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Error: Missing required argument");
      verifyNoInteractions(modules.client().namespace());
    }

    @Test
    void givenCatalogAndNamespace_whenMissingDataSource_thenFails() {
      int exitCode =
          new CommandLine(cliRoot)
              .execute(
                  args(
                      "namespace",
                      "describe",
                      "--catalog",
                      "test_catalog",
                      "--namespace",
                      "test_namespace"));
      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Error: Missing required argument");
      verifyNoInteractions(modules.client().namespace());
    }

    @Test
    void givenDataSourceAndNamespace_whenMissingCatalog_thenFails() {
      int exitCode =
          new CommandLine(cliRoot)
              .execute(
                  args(
                      "namespace",
                      "describe",
                      "--data-source",
                      "test_datasource",
                      "--namespace",
                      "test_namespace"));
      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Error: Missing required argument");
      verifyNoInteractions(modules.client().namespace());
    }
  }

  @Nested
  class ByIdParameters {
    @Test
    void givenNamespaceId_whenFound_thenReturnNamespace() {
      NamespaceClient useCaseMock = modules.client().namespace();
      when(useCaseMock.findNamespaceById(namespaceId)).thenReturn(Optional.of(namespace));

      int exitCode =
          new CommandLine(cliRoot)
              .execute(args("namespace", "describe", "--namespace-id", namespaceId.toString()));

      assertThat(exitCode).isEqualTo(ExitCode.OK);
      assertThat(printer.getOutput())
          .contains("\"message\":\"Success\"")
          .contains("test_namespace");
      verify(useCaseMock).findNamespaceById(namespaceId);
      verifyNoMoreInteractions(useCaseMock);
    }

    @Test
    void givenNamespaceId_whenNotFound_thenReturnNotFoundError() {
      NamespaceClient useCaseMock = modules.client().namespace();
      when(useCaseMock.findNamespaceById(namespaceId)).thenReturn(Optional.empty());

      int exitCode =
          new CommandLine(cliRoot)
              .execute(args("namespace", "describe", "--namespace-id", namespaceId.toString()));

      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(printer.getOutput()).startsWith("Namespace was not found");
      verify(useCaseMock).findNamespaceById(namespaceId);
      verifyNoMoreInteractions(useCaseMock);
    }

    @Test
    void givenInvalidUuidFormat_thenFails() {
      int exitCode =
          new CommandLine(cliRoot)
              .execute(args("namespace", "describe", "--namespace-id", "not-a-valid-uuid"));

      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Invalid value for option '--namespace-id'");
      verifyNoInteractions(modules.client().namespace());
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
                      "namespace",
                      "describe",
                      "--catalog",
                      "test_catalog",
                      "--data-source",
                      "test_datasource",
                      "--namespace",
                      "test_namespace",
                      "--namespace-id",
                      namespaceId.toString()));
      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Error: expected only one match but got");
      verifyNoInteractions(modules.client().namespace());
    }
  }

  @Nested
  class NoParameters {
    @Test
    void givenNoParams_thenFails() {
      int exitCode = new CommandLine(cliRoot).execute(args("namespace", "describe"));
      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Error: Missing required argument");
      verifyNoInteractions(modules.client().namespace());
    }
  }
}
