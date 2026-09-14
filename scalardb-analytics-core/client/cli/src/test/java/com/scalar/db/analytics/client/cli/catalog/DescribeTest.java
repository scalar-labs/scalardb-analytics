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

import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.client.cli.CliTestBase;
import com.scalar.db.analytics.sdk.catalog.CatalogClient;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

class DescribeTest extends CliTestBase {
  private final Catalog fakeCatalog = Catalog.create("test_catalog");
  private final UUID catalogId = fakeCatalog.getId();

  @Test
  void givenNoCatalogNameOrId_thenFails() {
    int exitCode = new CommandLine(cliRoot).execute(args("catalog", "describe"));
    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
    assertThat(getStderr()).contains("Error: Missing required argument");
  }

  @Test
  void givenBothCatalogNameAndId_thenFails() {
    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args(
                    "catalog",
                    "describe",
                    "--catalog",
                    "test_catalog",
                    "--catalog-id",
                    catalogId.toString()));
    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
    assertThat(getStderr()).contains("Error: expected only one match but got");
  }

  @Nested
  class GivenCatalogName {
    @Test
    void whenFound_thenReturnCatalog() {
      CatalogClient useCaseMock = modules.client().catalog();
      when(useCaseMock.findCatalogByName("test_catalog")).thenReturn(Optional.of(fakeCatalog));

      String[] args = {
        "--config", getConfigPath(), "catalog", "describe", "--catalog", "test_catalog"
      };
      int exitCode = new CommandLine(cliRoot).execute(args);

      assertThat(exitCode).isEqualTo(ExitCode.OK);
      assertThat(printer.getOutput())
          .contains("\"message\":\"Success\"")
          .contains("\"name\":\"" + fakeCatalog.getName() + "\"")
          .doesNotContain("tenantId");
      verify(useCaseMock).findCatalogByName("test_catalog");
      verifyNoMoreInteractions(useCaseMock);
    }

    @Test
    void whenNotFound_thenReturnNotFoundError() {
      CatalogClient useCaseMock = modules.client().catalog();
      when(useCaseMock.findCatalogByName("test_catalog")).thenReturn(Optional.empty());

      String[] args = {
        "--config", getConfigPath(), "catalog", "describe", "--catalog", "test_catalog"
      };
      int exitCode = new CommandLine(cliRoot).execute(args);

      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(printer.getOutput()).startsWith("Catalog was not found");
    }
  }

  @Nested
  class GivenCatalogId {
    @Test
    void whenFound_thenReturnCatalog() {
      CatalogClient useCaseMock = modules.client().catalog();
      when(useCaseMock.findCatalogById(catalogId)).thenReturn(Optional.of(fakeCatalog));

      int exitCode =
          new CommandLine(cliRoot)
              .execute(args("catalog", "describe", "--catalog-id", catalogId.toString()));

      assertThat(exitCode).isEqualTo(ExitCode.OK);
      assertThat(printer.getOutput())
          .contains("\"message\":\"Success\"")
          .contains("\"name\":\"" + fakeCatalog.getName() + "\"")
          .doesNotContain("tenantId");
      verify(useCaseMock).findCatalogById(catalogId);
      verifyNoMoreInteractions(useCaseMock);
    }

    @Test
    void whenNotFound_thenReturnNotFoundError() {
      CatalogClient useCaseMock = modules.client().catalog();
      when(useCaseMock.findCatalogById(catalogId)).thenReturn(Optional.empty());

      int exitCode =
          new CommandLine(cliRoot)
              .execute(args("catalog", "describe", "--catalog-id", catalogId.toString()));

      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(printer.getOutput()).startsWith("Catalog was not found");
      verify(useCaseMock).findCatalogById(catalogId);
      verifyNoMoreInteractions(useCaseMock);
    }

    @Test
    void givenInvalidUuidFormat_thenFails() {
      int exitCode =
          new CommandLine(cliRoot)
              .execute(args("catalog", "describe", "--catalog-id", "not-a-valid-uuid"));

      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Invalid value for option '--catalog-id'");
      verifyNoInteractions(modules.client().catalog());
    }

    @Test
    void whenOnlyCatalogIdOption_thenFails() {
      // When only catalog ID option is provided without value
      int exitCode = new CommandLine(cliRoot).execute(args("catalog", "describe"));

      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Error: Missing required argument");
    }
  }
}
