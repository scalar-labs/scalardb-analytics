/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider;
import com.scalar.db.analytics.client.cli.CliTestBase;
import com.scalar.db.analytics.sdk.datasource.DataSourceClient;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

class ListTest extends CliTestBase {
  private final UUID dataSourceId = UUID.randomUUID();
  private final UUID catalogId = UUID.randomUUID();
  private final DataSource dataSource =
      new DataSource(
          dataSourceId,
          catalogId,
          "test_datasource",
          ScalarDbProvider.builder()
              .configs(Collections.singletonMap("scalar.db.storage", "cassandra"))
              .build());

  @Test
  void shouldReturnDataSources() {
    DataSourceClient useCaseMock = modules.client().dataSource();
    when(useCaseMock.listDataSourcesByCatalog(any()))
        .thenReturn(Collections.singletonList(dataSource));

    int exitCode =
        new CommandLine(cliRoot).execute(args("data-source", "list", "-c", "test_catalog"));
    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.toString()).contains("test_datasource");
    verify(useCaseMock).listDataSourcesByCatalog("test_catalog");
    verifyNoMoreInteractions(useCaseMock);
  }

  @Test
  void shouldReturnEmptyWhenNoDataSources() {
    DataSourceClient useCaseMock = modules.client().dataSource();
    when(useCaseMock.listDataSourcesByCatalog(any())).thenReturn(Collections.emptyList());

    int exitCode =
        new CommandLine(cliRoot).execute(args("data-source", "list", "-c", "test_catalog"));
    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.toString()).contains("Success");
    verify(useCaseMock).listDataSourcesByCatalog("test_catalog");
    verifyNoMoreInteractions(useCaseMock);
  }
}
