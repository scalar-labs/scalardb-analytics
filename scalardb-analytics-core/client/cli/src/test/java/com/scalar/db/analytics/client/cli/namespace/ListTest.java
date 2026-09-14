/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.namespace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.DataSourceNamespace;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider;
import com.scalar.db.analytics.client.cli.CliTestBase;
import com.scalar.db.analytics.sdk.namespace.NamespaceClient;
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

  private final DataSourceNamespace dataSourceNamespace =
      new DataSourceNamespace(dataSource, namespace);

  @Test
  void shouldReturnNamespaces() {
    NamespaceClient useCaseMock = modules.client().namespace();
    when(useCaseMock.listNamespacesByCatalog(any()))
        .thenReturn(Collections.singletonList(dataSourceNamespace));

    int exitCode =
        new CommandLine(cliRoot).execute(args("namespace", "list", "-c", "test_catalog"));
    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.toString()).contains("test_namespace");
    verify(useCaseMock).listNamespacesByCatalog("test_catalog");
    verifyNoMoreInteractions(useCaseMock);
  }

  @Test
  void shouldReturnEmptyWhenNoNamespaces() {
    NamespaceClient useCaseMock = modules.client().namespace();
    when(useCaseMock.listNamespacesByCatalog(any())).thenReturn(Collections.emptyList());

    int exitCode =
        new CommandLine(cliRoot).execute(args("namespace", "list", "-c", "test_catalog"));
    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.toString()).contains("Success");
    verify(useCaseMock).listNamespacesByCatalog("test_catalog");
    verifyNoMoreInteractions(useCaseMock);
  }
}
