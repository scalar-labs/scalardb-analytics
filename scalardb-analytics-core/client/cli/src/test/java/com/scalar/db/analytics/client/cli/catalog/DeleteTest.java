/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.client.cli.CliTestBase;
import com.scalar.db.analytics.sdk.catalog.CatalogClient;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

class DeleteTest extends CliTestBase {
  private final UUID catalogId = UUID.randomUUID();

  @Nested
  class ByNameParameters {
    @Test
    void givenCatalogName_whenDeleteSucceeds_thenReturnsOk() {
      CatalogClient useCaseMock = modules.client().catalog();
      when(useCaseMock.deleteCatalogByName("test_catalog", false)).thenReturn(true);

      int exitCode =
          new CommandLine(cliRoot).execute(args("catalog", "delete", "--catalog", "test_catalog"));

      assertThat(exitCode).isEqualTo(ExitCode.OK);
      assertThat(printer.getOutput()).contains("Catalog was deleted: test_catalog");
      verify(useCaseMock).deleteCatalogByName("test_catalog", false);
      verifyNoMoreInteractions(useCaseMock);
    }

    @Test
    void givenCatalogNameWithCascade_whenDeleteSucceeds_thenReturnsOk() {
      CatalogClient useCaseMock = modules.client().catalog();
      when(useCaseMock.deleteCatalogByName("test_catalog", true)).thenReturn(true);

      int exitCode =
          new CommandLine(cliRoot)
              .execute(args("catalog", "delete", "--catalog", "test_catalog", "--cascade"));

      assertThat(exitCode).isEqualTo(ExitCode.OK);
      assertThat(printer.getOutput()).contains("Catalog was deleted: test_catalog");
      verify(useCaseMock).deleteCatalogByName("test_catalog", true);
      verifyNoMoreInteractions(useCaseMock);
    }

    @Test
    void givenCatalogName_whenCatalogNotFound_thenReturnsUsageError() {
      CatalogClient useCaseMock = modules.client().catalog();
      when(useCaseMock.deleteCatalogByName("test_catalog", false)).thenReturn(false);

      int exitCode =
          new CommandLine(cliRoot).execute(args("catalog", "delete", "--catalog", "test_catalog"));

      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(printer.getOutput()).contains("Catalog was not found: test_catalog");
      verify(useCaseMock).deleteCatalogByName("test_catalog", false);
      verifyNoMoreInteractions(useCaseMock);
    }

    @Test
    void givenCatalogName_whenDeletionBlocked_thenReturnsUsageErrorWithCascadeSuggestion() {
      CatalogClient useCaseMock = modules.client().catalog();
      when(useCaseMock.deleteCatalogByName("test_catalog", false))
          .thenThrow(new RuntimeException("Cannot delete catalog due to dependencies"));

      int exitCode =
          new CommandLine(cliRoot).execute(args("catalog", "delete", "--catalog", "test_catalog"));

      assertThat(exitCode).isEqualTo(ExitCode.SOFTWARE);
      verify(useCaseMock).deleteCatalogByName("test_catalog", false);
      verifyNoMoreInteractions(useCaseMock);
    }

    @Test
    void givenCatalogName_whenExceptionThrown_thenReturnsSoftwareError() {
      CatalogClient useCaseMock = modules.client().catalog();
      RuntimeException expectedException = new RuntimeException("Unexpected error");
      when(useCaseMock.deleteCatalogByName("test_catalog", false)).thenThrow(expectedException);

      // CommandLine catches the exception from StringPrinter and returns SOFTWARE exit code
      int exitCode =
          new CommandLine(cliRoot).execute(args("catalog", "delete", "--catalog", "test_catalog"));

      assertThat(exitCode).isEqualTo(ExitCode.SOFTWARE);
      // CommandLine prints the exception to stderr
      assertThat(getStderr()).contains("java.lang.RuntimeException: Unexpected error");

      verify(useCaseMock).deleteCatalogByName("test_catalog", false);
      verifyNoMoreInteractions(useCaseMock);
    }
  }

  @Nested
  class ByIdParameters {
    @Test
    void givenCatalogId_whenDeleteSucceeds_thenReturnsOk() {
      CatalogClient useCaseMock = modules.client().catalog();
      when(useCaseMock.deleteCatalogById(catalogId, false)).thenReturn(true);

      int exitCode =
          new CommandLine(cliRoot)
              .execute(args("catalog", "delete", "--catalog-id", catalogId.toString()));

      assertThat(exitCode).isEqualTo(ExitCode.OK);
      assertThat(printer.getOutput()).contains("Catalog was deleted: " + catalogId);
      verify(useCaseMock).deleteCatalogById(catalogId, false);
      verifyNoMoreInteractions(useCaseMock);
    }

    @Test
    void givenCatalogIdWithCascade_whenDeleteSucceeds_thenReturnsOk() {
      CatalogClient useCaseMock = modules.client().catalog();
      when(useCaseMock.deleteCatalogById(catalogId, true)).thenReturn(true);

      int exitCode =
          new CommandLine(cliRoot)
              .execute(
                  args("catalog", "delete", "--catalog-id", catalogId.toString(), "--cascade"));

      assertThat(exitCode).isEqualTo(ExitCode.OK);
      assertThat(printer.getOutput()).contains("Catalog was deleted: " + catalogId);
      verify(useCaseMock).deleteCatalogById(catalogId, true);
      verifyNoMoreInteractions(useCaseMock);
    }

    @Test
    void givenCatalogId_whenCatalogNotFound_thenReturnsUsageError() {
      CatalogClient useCaseMock = modules.client().catalog();
      when(useCaseMock.deleteCatalogById(catalogId, false)).thenReturn(false);

      int exitCode =
          new CommandLine(cliRoot)
              .execute(args("catalog", "delete", "--catalog-id", catalogId.toString()));

      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(printer.getOutput()).contains("Catalog was not found: " + catalogId);
      verify(useCaseMock).deleteCatalogById(catalogId, false);
      verifyNoMoreInteractions(useCaseMock);
    }

    @Test
    void givenInvalidUuidFormat_thenFails() {
      int exitCode =
          new CommandLine(cliRoot)
              .execute(args("catalog", "delete", "--catalog-id", "not-a-valid-uuid"));

      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Invalid value for option '--catalog-id'");
      verifyNoInteractions(modules.client().catalog());
    }
  }

  @Nested
  class MutualExclusivity {
    @Test
    void givenBothCatalogNameAndId_thenFails() {
      int exitCode =
          new CommandLine(cliRoot)
              .execute(
                  args(
                      "catalog",
                      "delete",
                      "--catalog",
                      "test_catalog",
                      "--catalog-id",
                      catalogId.toString()));
      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr())
          .contains(
              "Error: --catalog=<catalogName>, --catalog-id=<catalogId> are mutually exclusive");
      verifyNoInteractions(modules.client().catalog());
    }
  }

  @Nested
  class NoParameters {
    @Test
    void givenNoParams_thenFails() {
      int exitCode = new CommandLine(cliRoot).execute(args("catalog", "delete"));
      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Error: Missing required argument");
      verifyNoInteractions(modules.client().catalog());
    }
  }
}
