/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.client.cli.CliTestBase;
import com.scalar.db.analytics.sdk.catalog.CatalogClient;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

class CreateTest extends CliTestBase {
  private final Catalog fakeCatalog = Catalog.create("test_catalog");

  @Test
  void givenNoCatalogName_thenFails() {
    int exitCode = new CommandLine(cliRoot).execute(args("catalog", "create"));
    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
  }

  @Test
  void givenCatalogName_thenCallCreateCatalog() {
    CatalogClient useCaseMock = modules.client().catalog();
    when(useCaseMock.createCatalog(any())).thenReturn(fakeCatalog);

    int exitCode =
        new CommandLine(cliRoot).execute(args("catalog", "create", "--catalog", "test_catalog"));

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.toString()).contains(fakeCatalog.getName()).doesNotContain("tenantId");
    verify(useCaseMock).createCatalog("test_catalog");
    verifyNoMoreInteractions(useCaseMock);
  }
}
