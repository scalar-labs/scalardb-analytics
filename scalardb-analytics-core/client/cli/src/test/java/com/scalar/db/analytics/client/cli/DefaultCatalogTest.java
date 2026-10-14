/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

class DefaultCatalogTest extends CliTestBase {
  private static final String SERVER_PROPERTIES =
      "scalar.db.analytics.client.server.host=localhost\n"
          + "scalar.db.analytics.client.server.catalog.port=3456\n";

  @TempDir
  @SuppressWarnings("NotNullFieldNotInitialized")
  Path tempDir;

  @Nullable private Path configFile;

  @Override
  protected String getConfigPath() {
    return Objects.requireNonNull(configFile, "writeConfig must be called first").toString();
  }

  private void writeConfig(String extraProperties) throws IOException {
    configFile = tempDir.resolve("client.properties");
    Files.write(configFile, (SERVER_PROPERTIES + extraProperties).getBytes(StandardCharsets.UTF_8));
  }

  private int execute(String... commandArgs) {
    return new CommandLine(cliRoot).execute(args(commandArgs));
  }

  @Nested
  class WithDefaultCatalog {
    @BeforeEach
    void writeConfigWithDefault() throws IOException {
      writeConfig("scalar.db.analytics.client.default.catalog=default_catalog\n");
    }

    @Test
    void namespaceList_withoutCatalog_shouldUseDefault() {
      assertThat(execute("namespace", "list")).isEqualTo(ExitCode.OK);
      verify(modules.client().namespace()).listNamespacesByCatalog("default_catalog");
    }

    @Test
    void namespaceList_withCatalog_shouldPreferOption() {
      assertThat(execute("namespace", "list", "-c", "other")).isEqualTo(ExitCode.OK);
      verify(modules.client().namespace()).listNamespacesByCatalog("other");
    }

    @Test
    void dataSourceList_withoutCatalog_shouldUseDefault() {
      assertThat(execute("data-source", "list")).isEqualTo(ExitCode.OK);
      verify(modules.client().dataSource()).listDataSourcesByCatalog("default_catalog");
    }

    @Test
    void tableList_withoutCatalog_shouldUseDefault() {
      assertThat(execute("table", "list")).isEqualTo(ExitCode.OK);
      verify(modules.client().table()).listTablesByCatalog("default_catalog");
    }

    @Test
    void dataSourceDescribe_withoutCatalog_shouldUseDefault() {
      execute("data-source", "describe", "-d", "ds");
      verify(modules.client().dataSource()).findDataSourceByName("default_catalog", "ds");
    }

    @Test
    void namespaceDescribe_withoutCatalog_shouldUseDefault() {
      execute("namespace", "describe", "-d", "ds", "-n", "ns");
      verify(modules.client().namespace())
          .findNamespaceByName("default_catalog", "ds", Collections.singletonList("ns"));
    }

    @Test
    void tableDescribe_withoutCatalog_shouldUseDefault() {
      execute("table", "describe", "-d", "ds", "-n", "ns", "-t", "tbl");
      verify(modules.client().table())
          .describeTableByName("default_catalog", "ds", Collections.singletonList("ns"), "tbl");
    }

    @Test
    void dataSourceUnregister_withoutCatalog_shouldUseDefault() {
      when(modules.client().dataSource().deleteDataSourceByName("default_catalog", "ds", false))
          .thenReturn(true);

      assertThat(execute("data-source", "unregister", "-d", "ds")).isEqualTo(ExitCode.OK);
      assertThat(printer.getOutput()).contains("Data source unregistered: default_catalog.ds");
    }

    @Test
    void permissionGrantTable_withoutCatalog_shouldUseDefault() {
      assertThat(
              execute(
                  "permission",
                  "grant",
                  "table",
                  "-p",
                  "read",
                  "--user",
                  "alice",
                  "-d",
                  "ds",
                  "-n",
                  "ns",
                  "-t",
                  "tbl"))
          .isEqualTo(ExitCode.OK);
      verify(modules.client().permission())
          .grantTablePermission(
              "USER",
              "alice",
              "TABLE_READ",
              "default_catalog",
              "ds",
              Collections.singletonList("ns"),
              "tbl");
    }

    @Test
    void permissionRevokeDataSource_withoutCatalog_shouldUseDefault() {
      assertThat(
              execute(
                  "permission", "revoke", "data-source", "-p", "read", "--role", "r", "-d", "ds"))
          .isEqualTo(ExitCode.OK);
      verify(modules.client().permission())
          .revokeDataSourcePermission("ROLE", "r", "DATA_SOURCE_READ", "default_catalog", "ds");
    }

    @Test
    void catalogDescribe_withoutCatalog_shouldStillRequireIt() {
      assertThat(execute("catalog", "describe")).isEqualTo(ExitCode.USAGE);
      verifyNoInteractions(modules.client().catalog());
    }
  }

  @Nested
  class WithoutDefaultCatalog {
    @Test
    void namespaceList_withoutCatalog_shouldFailAsBefore() throws IOException {
      writeConfig("");

      assertThat(execute("namespace", "list")).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr()).contains("Missing required option: '--catalog=<catalog>'");
      verifyNoInteractions(modules.client().namespace());
    }

    @Test
    void dataSourceUnregister_withoutCatalog_shouldFailAsBefore() throws IOException {
      writeConfig("");

      assertThat(execute("data-source", "unregister", "-d", "ds")).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr())
          .contains("Error: Missing required argument(s): --catalog=<catalogName>");
      verifyNoInteractions(modules.client().dataSource());
    }

    @Test
    void namespaceList_withInvalidConfig_shouldReportMissingCatalog() throws IOException {
      configFile = tempDir.resolve("client.properties");
      Files.write(
          configFile,
          "scalar.db.analytics.client.server.host=localhost\n".getBytes(StandardCharsets.UTF_8));

      assertThat(execute("namespace", "list")).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr())
          .contains("Missing required option: '--catalog=<catalog>'")
          .contains("the default catalog could not be read")
          .contains("scalar.db.analytics.client.server.catalog.port");
      verifyNoInteractions(modules.client().namespace());
    }

    @Test
    void namespaceList_withDefaultButInvalidConfig_shouldReportTheConfigError() throws IOException {
      // A default is set, but the configuration is invalid (username without password).
      writeConfig(
          "scalar.db.analytics.client.default.catalog=default_catalog\n"
              + "scalar.db.analytics.client.auth.username=alice\n");

      assertThat(execute("namespace", "list")).isEqualTo(ExitCode.USAGE);
      assertThat(getStderr())
          .contains("the default catalog could not be read")
          .contains("scalar.db.analytics.client.auth.password");
      verifyNoInteractions(modules.client().namespace());
    }

    @Test
    void namespaceList_withBlankDefault_shouldTreatItAsUnset() throws IOException {
      writeConfig("scalar.db.analytics.client.default.catalog=  \n");

      assertThat(execute("namespace", "list")).isEqualTo(ExitCode.USAGE);
      verifyNoInteractions(modules.client().namespace());
    }
  }
}
