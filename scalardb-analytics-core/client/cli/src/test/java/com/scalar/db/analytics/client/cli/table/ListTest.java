/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTable;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.Table;
import com.scalar.db.analytics.api.model.TableInfo;
import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider;
import com.scalar.db.analytics.client.cli.CliTestBase;
import com.scalar.db.analytics.sdk.table.TableClient;
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

  private final UUID namespaceId = UUID.randomUUID();
  private final Namespace namespace =
      new Namespace(namespaceId, dataSourceId, Collections.singletonList("test_namespace"));

  private final UUID tableId = UUID.randomUUID();
  private final TableInfo tableInfo = new TableInfo(tableId, namespaceId, "test_table");
  private final Table table = new Table(tableInfo);

  private final DataSourceNamespaceTable dataSourceNamespaceTable =
      new DataSourceNamespaceTable(dataSource, namespace, table);

  @Test
  void shouldReturnTables() {
    TableClient useCaseMock = modules.client().table();
    when(useCaseMock.listTablesByCatalog(any()))
        .thenReturn(Collections.singletonList(dataSourceNamespaceTable));

    int exitCode = new CommandLine(cliRoot).execute(args("table", "list", "-c", "test_catalog"));
    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.toString()).contains("test_table");
    verify(useCaseMock).listTablesByCatalog("test_catalog");
    verifyNoMoreInteractions(useCaseMock);
  }

  @Test
  void shouldReturnEmptyWhenNoTables() {
    TableClient useCaseMock = modules.client().table();
    when(useCaseMock.listTablesByCatalog(any())).thenReturn(Collections.emptyList());

    int exitCode = new CommandLine(cliRoot).execute(args("table", "list", "-c", "test_catalog"));
    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.toString()).contains("Success");
    verify(useCaseMock).listTablesByCatalog("test_catalog");
    verifyNoMoreInteractions(useCaseMock);
  }
}
